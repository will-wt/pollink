package com.nova.pollink.server.config;

import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.server.interfaces.controller.PollController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server 优雅关闭配置。
 * 按以下顺序处理关闭：
 * 1. 停止接收新的长轮询请求
 * 2. 唤醒所有已 hold 的客户端连接
 * 3. 关闭 gRPC 节点间连接
 * 4. 等待处理中的请求完成
 * 5. 从注册中心注销自身
 * @author wentao
 */
@Component
public class GracefulShutdownConfig implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownConfig.class);

    private final PollController pollController;
    private final DiscoveryService discoveryService;
    private final GrpcServerConfig grpcServerConfig;

    private volatile boolean running = false;
    private final AtomicBoolean acceptingRequests = new AtomicBoolean(true);

    public GracefulShutdownConfig(PollController pollController,
                                  DiscoveryService discoveryService,
                                  GrpcServerConfig grpcServerConfig) {
        this.pollController = pollController;
        this.discoveryService = discoveryService;
        this.grpcServerConfig = grpcServerConfig;
    }

    @Override
    public void start() {
        this.running = true;
        this.acceptingRequests.set(true);
        log.info("Server graceful shutdown handler initialized");
    }

    /**
     * 查询当前是否正在接收新的长轮询请求。
     */
    public boolean isAcceptingRequests() {
        return acceptingRequests.get();
    }

    @Override
    public void stop() {
        doShutdown(null);
    }

    @Override
    public void stop(Runnable callback) {
        doShutdown(callback);
    }

    private void doShutdown(Runnable callback) {
        log.info("Starting graceful shutdown...");

        // Step 1: 停止接收新的长轮询请求
        acceptingRequests.set(false);
        log.info("Stopped accepting new poll requests");

        // Step 2: 唤醒所有 hold 的客户端连接
        pollController.wakeupAllPendingPolls();

        // Step 3: 关闭 gRPC 服务端（停止接收节点间通信）
        grpcServerConfig.stopGrpcServer();

        // Step 4: 等待片刻让处理中的请求完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 5: 从注册中心注销
        String selfIp = getSelfIp();
        discoveryService.deregister(selfIp);
        log.info("Server deregistered from discovery");

        this.running = false;
        log.info("Graceful shutdown completed");

        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 确保在其他 SmartLifecycle bean 之后执行
        return Integer.MAX_VALUE - 10;
    }

    private String getSelfIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
