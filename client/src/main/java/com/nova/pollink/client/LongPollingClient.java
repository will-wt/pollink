package com.nova.pollink.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.pollink.client.handler.ConfigHandler;
import com.nova.pollink.client.handler.MessageHandler;
import com.nova.pollink.client.model.Config;
import com.nova.pollink.client.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 长轮询客户端。
 * 按配置的时间间隔向 Server 发起 HTTP 长轮询请求，
 * 接收消息后通过回调分发给业务处理器。
 *
 * <p>线程模型：单个后台线程负责轮询，回调在独立的线程池执行，避免阻塞轮询。</p>
 */
public class LongPollingClient {

    private static final Logger log = LoggerFactory.getLogger(LongPollingClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String serverUrl;
    private final String clientId;
    private final int pollIntervalSeconds;
    private final int requestTimeoutSeconds;
    private final Set<String> subscribeTopics;
    private final MessageHandler messageHandler;
    private final ConfigHandler configHandler;

    private final HttpClient httpClient;
    private final ExecutorService callbackExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastMessageId = new AtomicLong(0);
    private final AtomicReference<Integer> lastConfigVersion = new AtomicReference<>(0);

    LongPollingClient(String serverUrl, String clientId,
                      int pollIntervalSeconds, int requestTimeoutSeconds,
                      Set<String> subscribeTopics,
                      MessageHandler messageHandler, ConfigHandler configHandler) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.clientId = clientId;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.subscribeTopics = subscribeTopics;
        this.messageHandler = messageHandler;
        this.configHandler = configHandler;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.callbackExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "nova-client-callback");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动客户端，开始轮询。
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // 启动消息轮询线程
            for (String topic : subscribeTopics) {
                new Thread(() -> pollMessages(topic), "nova-poll-" + topic).start();
            }
            // 启动配置轮询线程
            new Thread(this::pollConfigs, "nova-poll-config").start();
            log.info("LongPollingClient started for clientId={}", clientId);
        }
    }

    /**
     * 优雅关闭客户端。
     * 等待当前轮询完成后退出，不直接断开连接。
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            callbackExecutor.shutdown();
            try {
                if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    callbackExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                callbackExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("LongPollingClient shut down for clientId={}", clientId);
        }
    }

    private void pollMessages(String topic) {
        while (running.get()) {
            try {
                String url = serverUrl + "/api/v1/poll/messages?clientId=" + clientId
                    + "&topic=" + topic + "&lastId=" + lastMessageId.get();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ApiResponse<List<Message>> apiResponse = mapper.readValue(response.body(),
                        new TypeReference<ApiResponse<List<Message>>>() {});

                    if (apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                        for (Message msg : apiResponse.getData()) {
                            lastMessageId.set(Math.max(lastMessageId.get(), msg.getId()));
                            if (messageHandler != null) {
                                callbackExecutor.submit(() -> {
                                    try {
                                        messageHandler.handle(msg);
                                    } catch (Exception e) {
                                        log.error("Message handler error: {}", e.getMessage(), e);
                                    }
                                });
                            }
                        }
                    }
                } else if (response.statusCode() == 503) {
                    log.warn("Server unavailable (503), backing off...");
                    Thread.sleep(5000);
                }

                // 轮询间隔
                Thread.sleep(pollIntervalSeconds * 1000L);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Poll error for topic {}: {}", topic, e.getMessage());
                // 指数退避重试
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void pollConfigs() {
        while (running.get()) {
            try {
                String url = serverUrl + "/api/v1/poll/configs?clientId=" + clientId
                    + "&version=" + lastConfigVersion.get();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ApiResponse<List<Config>> apiResponse = mapper.readValue(response.body(),
                        new TypeReference<ApiResponse<List<Config>>>() {});

                    if (apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                        for (Config config : apiResponse.getData()) {
                            lastConfigVersion.set(Math.max(lastConfigVersion.get(), config.getVersion()));
                            if (configHandler != null) {
                                callbackExecutor.submit(() -> {
                                    try {
                                        configHandler.handle(config);
                                    } catch (Exception e) {
                                        log.error("Config handler error: {}", e.getMessage(), e);
                                    }
                                });
                            }
                        }
                    }
                }

                Thread.sleep(pollIntervalSeconds * 1000L);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Config poll error: {}", e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 内部使用的 API 响应包装类。
     */
    private static class ApiResponse<T> {
        private int code;
        private T data;
        private boolean hasMore;

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }

        public boolean isHasMore() { return hasMore; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    }
}
