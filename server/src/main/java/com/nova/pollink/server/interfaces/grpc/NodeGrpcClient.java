package com.nova.pollink.server.interfaces.grpc;

import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.model.ServerNode;
import com.nova.pollink.server.proto.NodeProto;
import com.nova.pollink.server.proto.NodeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端，负责与其他 server 节点建立连接并发送数据通知。
 * 维护到每个活跃节点的持久双向流连接。
 */
@Component
public class NodeGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(NodeGrpcClient.class);

    private final DiscoveryService discoveryService;
    private final int grpcPort;

    /** 已建立的节点连接：key = nodeId, value = StreamObserver */
    private final Map<String, StreamObserver<NodeProto.NodeMessage>> peerStreams = new ConcurrentHashMap<>();
    private final Map<String, ManagedChannel> peerChannels = new ConcurrentHashMap<>();

    public NodeGrpcClient(DiscoveryService discoveryService,
                          @Value("${nova.server.grpc-port:9101}") int grpcPort) {
        this.discoveryService = discoveryService;
        this.grpcPort = grpcPort;
    }

    @PostConstruct
    public void init() {
        // 启动后延迟 3 秒再连接其他节点，等待自身注册完成
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                refreshConnections();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 每 30 秒刷新一次连接（处理节点上下线）
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::refreshConnections, 10, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        peerChannels.values().forEach(channel -> {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 向所有其他活跃节点广播数据通知。
     *
     * @param dataId 数据 ID
     * @param dataType 数据类型（MESSAGE 或 CONFIG）
     * @param topic 业务 topic
     */
    public void notifyPeers(String dataId, NodeProto.DataType dataType, String topic) {
        NodeProto.NodeMessage msg = NodeProto.NodeMessage.newBuilder()
            .setType(NodeProto.MessageType.DATA_NOTIFY)
            .setDataId(dataId)
            .setDataType(dataType)
            .setTopic(topic)
            .build();

        peerStreams.forEach((nodeId, stream) -> {
            try {
                stream.onNext(msg);
            } catch (Exception e) {
                log.warn("Failed to notify peer {}: {}", nodeId, e.getMessage());
                // 连接可能已断开，下次 refreshConnections 会重建
            }
        });
    }

    /**
     * 刷新与其他节点的连接。
     * 新节点加入时建立连接，已离线节点关闭连接。
     */
    private void refreshConnections() {
        var activeNodes = discoveryService.listActiveNodes();
        String selfIp = getSelfIp();

        for (ServerNode node : activeNodes) {
            String nodeId = node.getId();
            if (node.getIp().equals(selfIp)) {
                continue; // 跳过自己
            }
            if (!peerStreams.containsKey(nodeId)) {
                connectToPeer(node);
            }
        }

        // 清理已不在活跃列表中的连接
        peerStreams.keySet().stream()
            .filter(id -> activeNodes.stream().noneMatch(n -> n.getId().equals(id)))
            .forEach(this::disconnectPeer);
    }

    private void connectToPeer(ServerNode node) {
        String address = node.getIp() + ":" + grpcPort;
        try {
            ManagedChannel channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();

            NodeServiceGrpc.NodeServiceStub stub = NodeServiceGrpc.newStub(channel);
            StreamObserver<NodeProto.NodeMessage> requestStream = stub.stream(new StreamObserver<>() {
                @Override
                public void onNext(NodeProto.NodeMessage value) {
                    // 接收对方发来的消息（心跳等）
                }
                @Override
                public void onError(Throwable t) {
                    log.warn("Peer stream closed: {} - {}", node.getId(), t.getMessage());
                    disconnectPeer(node.getId());
                }
                @Override
                public void onCompleted() {
                    disconnectPeer(node.getId());
                }
            });

            peerChannels.put(node.getId(), channel);
            peerStreams.put(node.getId(), requestStream);
            log.info("Connected to peer node: {} at {}", node.getId(), address);
        } catch (Exception e) {
            log.error("Failed to connect to peer {}: {}", node.getId(), e.getMessage());
        }
    }

    private void disconnectPeer(String nodeId) {
        StreamObserver<NodeProto.NodeMessage> stream = peerStreams.remove(nodeId);
        if (stream != null) {
            try { stream.onCompleted(); } catch (Exception ignored) {}
        }
        ManagedChannel channel = peerChannels.remove(nodeId);
        if (channel != null) {
            channel.shutdownNow();
        }
        log.info("Disconnected from peer node: {}", nodeId);
    }

    private String getSelfIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
