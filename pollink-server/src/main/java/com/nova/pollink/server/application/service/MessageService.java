package com.nova.pollink.server.application.service;

import com.nova.pollink.server.domain.entity.Message;
import com.nova.pollink.server.domain.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息应用服务。
 * 编排消息的发送、查询和状态更新流程，不包含业务规则判断。
 * @author wentao
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 创建并保存新消息。
     *
     * @param topic 业务 topic
     * @param payload 消息内容
     * @param expireSeconds 过期时间（秒）
     * @return 保存后的消息（含生成 ID）
     */
    public Message createMessage(String topic, String payload, int expireSeconds) {
        Message message = new Message();
        message.setTopic(topic);
        message.setPayload(payload);
        message.setStatus(0);
        message.setCreateTime(LocalDateTime.now());
        message.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        messageRepository.save(message);
        return message;
    }

    /**
     * 清理已过期且未推送的消息。
     */
    public int cleanExpiredMessages() {
        return messageRepository.cleanExpired();
    }

    /**
     * 查询指定 topic 的待推送消息。
     */
    public List<Message> pollMessages(String topic, Long lastId, int limit) {
        return messageRepository.findPendingByTopic(topic, lastId, limit);
    }

    /**
     * 标记消息为已推送。
     */
    public void markDelivered(Long messageId) {
        messageRepository.updateStatus(messageId, 1);
    }
}
