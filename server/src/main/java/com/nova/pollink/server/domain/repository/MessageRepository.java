package com.nova.pollink.server.domain.repository;

import com.nova.pollink.server.domain.entity.Message;
import java.util.List;

/**
 * 消息仓储接口。
 * 定义领域层对消息数据的访问契约，由基础设施层实现。
 */
public interface MessageRepository {

    /**
     * 保存消息。
     */
    void save(Message message);

    /**
     * 按 topic 和状态查询待推送消息。
     */
    List<Message> findPendingByTopic(String topic, Long lastId, int limit);

    /**
     * 更新消息状态。
     */
    void updateStatus(Long id, int status);

    /**
     * 清理过期消息。
     */
    int cleanExpired();
}
