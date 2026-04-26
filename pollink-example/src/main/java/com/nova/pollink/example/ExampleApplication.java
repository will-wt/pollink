package com.nova.pollink.example;

import com.nova.pollink.client.LongPollingClient;
import com.nova.pollink.client.LongPollingClientBuilder;
import com.nova.pollink.server.ServerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import jakarta.annotation.PostConstruct;

/**
 * 示例启动类。
 * 同时启动 Server 和一个 Client，演示长轮询流程。
 */
@SpringBootApplication
public class ExampleApplication {

    private ConfigurableApplicationContext serverContext;

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    @PostConstruct
    public void startDemo() {
        // 启动 Server
        serverContext = SpringApplication.run(ServerApplication.class);

        // 启动 Client
        LongPollingClient client = LongPollingClientBuilder.builder()
            .serverUrl("http://localhost:8080")
            .clientId("demo-client-001")
            .pollIntervalSeconds(2)
            .subscribeTopics("demo_topic")
            .messageHandler(msg -> {
                System.out.println("[Client] Received message: " + msg.getPayload());
            })
            .configHandler(cfg -> {
                System.out.println("[Client] Received config: " + cfg.getKey() + " = " + cfg.getValue());
            })
            .build();

        client.start();
        System.out.println("[Example] Demo started. Server on :8080, Client polling...");
    }
}
