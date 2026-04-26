package com.nova.pollink.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pollink Server 启动类。
 * 提供 HTTP 长轮询接口和 gRPC 节点间通信服务。
 * @author wentao
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.nova.pollink.server", "com.nova.pollink.discovery"})
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
