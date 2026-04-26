package com.nova.pollink.server.infrastructure.repository;

import com.nova.pollink.server.domain.entity.Message;
import com.nova.pollink.server.domain.repository.MessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MessageRepository 的 MyBatis 实现。
 */
@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMapper messageMapper;

    public MessageRepositoryImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void save(Message message) {
        messageMapper.insert(message);
    }

    @Override
    public List<Message> findPendingByTopic(String topic, Long lastId, int limit) {
        return messageMapper.selectPendingByTopic(topic, lastId, limit);
    }

    @Override
    public void updateStatus(Long id, int status) {
        messageMapper.updateStatus(id, status);
    }

    @Override
    public int cleanExpired() {
        return messageMapper.cleanExpired();
    }
}
