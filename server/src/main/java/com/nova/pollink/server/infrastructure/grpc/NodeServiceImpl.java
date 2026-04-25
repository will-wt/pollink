package com.nova.pollink.server.infrastructure.grpc;

import com.nova.pollink.server.interfaces.controller.PollController;
import com.nova.pollink.server.proto.NodeProto;
import com.nova.pollink.server.proto.NodeServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * gRPC NodeService 服务端实现。
 * 接收其他节点发来的数据通知，唤醒本地 hold 的客户端连接。
 */
@Component
public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    private final PollController pollController;

    public NodeServiceImpl(PollController pollController) {
        this.pollController = pollController;
    }

    @Override
    public StreamObserver<NodeProto.NodeMessage> stream(StreamObserver<NodeProto.NodeMessage> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(NodeProto.NodeMessage msg) {
                if (msg.getType() == NodeProto.MessageType.DATA_NOTIFY) {
                    String topic = msg.getTopic();
                    log.info("Received data notify from peer node: dataId={}, type={}, topic={}",
                        msg.getDataId(), msg.getDataType(), topic);
                    // 唤醒本地等待该 topic 的客户端
                    pollController.wakeupPendingPolls(topic);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("gRPC stream error from peer: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
