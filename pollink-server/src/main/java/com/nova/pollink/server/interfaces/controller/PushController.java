package com.nova.pollink.server.interfaces.controller;

import com.nova.pollink.server.api.dto.PushConfigRequest;
import com.nova.pollink.server.api.dto.PushMessageRequest;
import com.nova.pollink.server.application.service.MessageService;
import com.nova.pollink.server.application.service.ConfigService;
import com.nova.pollink.server.domain.entity.Message;
import com.nova.pollink.server.domain.entity.Config;
import com.nova.pollink.server.interfaces.grpc.NodeGrpcClient;
import com.nova.pollink.server.proto.NodeProto;
import org.springframework.web.bind.annotation.*;

/**
 * 数据推送控制器。
 * 供 Admin 或业务系统调用，用于写入消息和发布配置。
 */
@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final MessageService messageService;
    private final ConfigService configService;
    private final PollController pollController;
    private final NodeGrpcClient nodeGrpcClient;

    public PushController(MessageService messageService,
                          ConfigService configService,
                          PollController pollController,
                          NodeGrpcClient nodeGrpcClient) {
        this.messageService = messageService;
        this.configService = configService;
        this.pollController = pollController;
        this.nodeGrpcClient = nodeGrpcClient;
    }

    /**
     * 推送消息。
     *
     * @param request 包含 topic、payload、expireSeconds
     * @return 创建的消息
     */
    @PostMapping("/message")
    public Message pushMessage(@RequestBody PushMessageRequest request) {
        Message message = messageService.createMessage(
            request.getTopic(), request.getPayload(), request.getExpireSeconds());
        // 唤醒等待该 topic 的客户端
        pollController.wakeupPendingPolls(request.getTopic());
        // 向其他节点广播通知
        nodeGrpcClient.notifyPeers(
            String.valueOf(message.getId()),
            NodeProto.DataType.MESSAGE,
            request.getTopic()
        );
        return message;
    }

    /**
     * 发布配置。
     *
     * @param request 包含 key、value
     * @return 创建的配置
     */
    @PostMapping("/config")
    public Config pushConfig(@RequestBody PushConfigRequest request) {
        Config config = configService.createConfig(request.getKey(), request.getValue());
        configService.publishConfig(config.getId());
        // 唤醒等待配置的客户端
        pollController.wakeupPendingPolls("config");
        // 向其他节点广播通知
        nodeGrpcClient.notifyPeers(
            String.valueOf(config.getId()),
            NodeProto.DataType.CONFIG,
            "config"
        );
        return config;
    }
}
