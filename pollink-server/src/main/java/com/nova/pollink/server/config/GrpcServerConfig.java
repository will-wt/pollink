package com.nova.pollink.server.config;

import com.nova.pollink.server.infrastructure.grpc.NodeServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 服务端配置。
 * 启动独立的 gRPC 服务监听节点间通信端口。
 * @author wentao
 */
@Configuration
public class GrpcServerConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);

    private final int grpcPort;
    private final NodeServiceImpl nodeServiceImpl;
    private Server grpcServer;

    public GrpcServerConfig(@Value("${nova.pollink.server.grpc-port:9101}") int grpcPort,
                            NodeServiceImpl nodeServiceImpl) {
        this.grpcPort = grpcPort;
        this.nodeServiceImpl = nodeServiceImpl;
    }

    @PostConstruct
    public void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(grpcPort)
            .addService(nodeServiceImpl)
            .build()
            .start();
        log.info("gRPC server started on port {}", grpcPort);
    }

    @PreDestroy
    public void stopGrpcServer() {
        if (grpcServer != null) {
            grpcServer.shutdown();
            try {
                if (!grpcServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    grpcServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                grpcServer.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("gRPC server stopped");
        }
    }
}
