package com.nova.pollink.client;

import com.nova.pollink.client.handler.ConfigHandler;
import com.nova.pollink.client.handler.MessageHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * LongPollingClient 的 Builder。
 * 提供流式 API 用于配置客户端参数。
 */
public class LongPollingClientBuilder {

    private String serverUrl;
    private String clientId;
    private int pollIntervalSeconds = 2;
    private int requestTimeoutSeconds = 35;
    private Set<String> subscribeTopics = new HashSet<>();
    private MessageHandler messageHandler;
    private ConfigHandler configHandler;

    public static LongPollingClientBuilder builder() {
        return new LongPollingClientBuilder();
    }

    public LongPollingClientBuilder serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public LongPollingClientBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public LongPollingClientBuilder pollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
        return this;
    }

    public LongPollingClientBuilder requestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        return this;
    }

    public LongPollingClientBuilder subscribeTopics(String... topics) {
        for (String topic : topics) {
            this.subscribeTopics.add(topic);
        }
        return this;
    }

    public LongPollingClientBuilder messageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        return this;
    }

    public LongPollingClientBuilder configHandler(ConfigHandler configHandler) {
        this.configHandler = configHandler;
        return this;
    }

    public LongPollingClient build() {
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("serverUrl is required");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }
        return new LongPollingClient(serverUrl, clientId, pollIntervalSeconds,
            requestTimeoutSeconds, subscribeTopics, messageHandler, configHandler);
    }
}
