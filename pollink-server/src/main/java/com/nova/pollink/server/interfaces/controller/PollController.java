package com.nova.pollink.server.interfaces.controller;

import com.nova.pollink.server.application.service.MessageService;
import com.nova.pollink.server.application.service.ConfigService;
import com.nova.pollink.server.config.GracefulShutdownConfig;
import com.nova.pollink.server.dal.entity.Message;
import com.nova.pollink.server.dal.entity.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 长轮询控制器。
 * 接收客户端长轮询请求，无数据时 hold 指定时间，有新数据时立即返回。
 */
@RestController
@RequestMapping("/api/v1/poll")
public class PollController {

    private final MessageService messageService;
    private final ConfigService configService;
    private final int pollTimeoutSeconds;
    private final GracefulShutdownConfig gracefulShutdownConfig;

    /** 消息轮询等待：key = "topic:clientId:seq", value = DeferredResult */
    private final ConcurrentHashMap<String, DeferredResult<List<Message>>> pendingMessagePolls = new ConcurrentHashMap<>();
    /** 配置轮询等待：key = "config:clientId:seq", value = DeferredResult */
    private final ConcurrentHashMap<String, DeferredResult<List<Config>>> pendingConfigPolls = new ConcurrentHashMap<>();
    private final AtomicLong pollSeq = new AtomicLong(0);

    public PollController(MessageService messageService,
                          ConfigService configService,
                          @Value("${nova.pollink.server.poll-timeout-seconds:30}") int pollTimeoutSeconds,
                          GracefulShutdownConfig gracefulShutdownConfig) {
        this.messageService = messageService;
        this.configService = configService;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
        this.gracefulShutdownConfig = gracefulShutdownConfig;
    }

    /**
     * 客户端拉取消息的长轮询接口。
     *
     * @param clientId 客户端唯一标识
     * @param topic 订阅的 topic
     * @param lastId 上次拉取的最大消息 ID（用于增量）
     * @return 消息列表（可能为空，表示超时）
     */
    @GetMapping("/messages")
    public DeferredResult<List<Message>> pollMessages(
            @RequestParam String clientId,
            @RequestParam String topic,
            @RequestParam(required = false, defaultValue = "0") Long lastId) {

        // Step 1 of graceful shutdown: 拒绝新的长轮询请求
        if (!gracefulShutdownConfig.isAcceptingRequests()) {
            DeferredResult<List<Message>> reject = new DeferredResult<>();
            reject.setErrorResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down"));
            return reject;
        }

        String key = topic + ":" + clientId + ":" + pollSeq.incrementAndGet();
        DeferredResult<List<Message>> result = new DeferredResult<>((long) pollTimeoutSeconds * 1000);

        // 立即查询是否有数据
        List<Message> messages = messageService.pollMessages(topic, lastId, 100);
        if (!messages.isEmpty()) {
            result.setResult(messages);
            return result;
        }

        // 无数据时 hold 住请求，仅依赖 DeferredResult 内置超时
        pendingMessagePolls.put(key, result);

        result.onCompletion(() -> pendingMessagePolls.remove(key));
        result.onTimeout(() -> {
            pendingMessagePolls.remove(key);
            result.setResult(List.of());
        });

        return result;
    }

    /**
     * 客户端拉取配置的长轮询接口。
     *
     * @param clientId 客户端唯一标识
     * @param lastVersion 上次同步的配置版本号
     * @return 配置列表（可能为空，表示超时）
     */
    @GetMapping("/configs")
    public DeferredResult<List<Config>> pollConfigs(
            @RequestParam String clientId,
            @RequestParam(required = false, defaultValue = "0") int lastVersion) {

        // Step 1 of graceful shutdown: 拒绝新的长轮询请求
        if (!gracefulShutdownConfig.isAcceptingRequests()) {
            DeferredResult<List<Config>> reject = new DeferredResult<>();
            reject.setErrorResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down"));
            return reject;
        }

        String key = "config:" + clientId + ":" + pollSeq.incrementAndGet();
        DeferredResult<List<Config>> result = new DeferredResult<>((long) pollTimeoutSeconds * 1000);

        List<Config> configs = configService.pollConfigs(lastVersion);
        if (!configs.isEmpty()) {
            result.setResult(configs);
            return result;
        }

        pendingConfigPolls.put(key, result);

        result.onCompletion(() -> pendingConfigPolls.remove(key));
        result.onTimeout(() -> {
            pendingConfigPolls.remove(key);
            result.setResult(List.of());
        });

        return result;
    }

    /**
     * 当有新数据到达时，唤醒对应 topic 的等待请求。
     * 由 gRPC 通知或数据写入接口调用。
     *
     * @param topic 数据 topic（配置类型传 "config"）
     */
    public void wakeupPendingPolls(String topic) {
        if ("config".equals(topic)) {
            pendingConfigPolls.forEach((key, deferred) -> {
                if (!deferred.isSetOrExpired()) {
                    deferred.setResult(List.of());
                }
            });
        } else {
            pendingMessagePolls.forEach((key, deferred) -> {
                if (key.startsWith(topic + ":") && !deferred.isSetOrExpired()) {
                    deferred.setResult(List.of());
                }
            });
        }
    }

    /**
     * 唤醒所有等待中的轮询请求（用于优雅关闭）。
     */
    public void wakeupAllPendingPolls() {
        pendingMessagePolls.forEach((key, deferred) -> {
            if (!deferred.isSetOrExpired()) {
                deferred.setResult(List.of());
            }
        });
        pendingConfigPolls.forEach((key, deferred) -> {
            if (!deferred.isSetOrExpired()) {
                deferred.setResult(List.of());
            }
        });
    }

    /**
     * 返回当前 hold 中的轮询请求数量，用于心跳上报连接数。
     */
    public int getPendingPollCount() {
        return pendingMessagePolls.size() + pendingConfigPolls.size();
    }
}
