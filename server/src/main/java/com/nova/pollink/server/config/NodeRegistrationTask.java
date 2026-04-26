package com.nova.pollink.server.config;

import com.nova.pollink.discovery.DiscoveryProperties;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.model.ServerNode;
import com.nova.pollink.server.interfaces.controller.PollController;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 节点注册和心跳任务。
 * 启动时向注册中心注册自身，之后定期发送心跳保持存活状态。
 */
@Component
public class NodeRegistrationTask {

    private static final Logger log = LoggerFactory.getLogger(NodeRegistrationTask.class);

    private final DiscoveryService discoveryService;
    private final DiscoveryProperties discoveryProperties;
    private final PollController pollController;
    private final ScheduledExecutorService heartbeatExecutor;

    public NodeRegistrationTask(DiscoveryService discoveryService,
                                DiscoveryProperties discoveryProperties,
                                PollController pollController) {
        this.discoveryService = discoveryService;
        this.discoveryProperties = discoveryProperties;
        this.pollController = pollController;
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "discovery-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void register() {
        String ip = getSelfIp();
        ServerNode node = new ServerNode(ip, ip);
        discoveryService.register(node);
        log.info("Server node registered: {}", ip);

        // 启动定时心跳（同时上报连接数）
        heartbeatExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    int connectionCount = pollController.getPendingPollCount();
                    discoveryService.updateConnectionCount(ip, connectionCount);
                    discoveryService.heartbeat(ip);
                } catch (Exception e) {
                    log.warn("Heartbeat failed: {}", e.getMessage());
                }
            },
            discoveryProperties.getHeartbeatIntervalSeconds(),
            discoveryProperties.getHeartbeatIntervalSeconds(),
            TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String getSelfIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
