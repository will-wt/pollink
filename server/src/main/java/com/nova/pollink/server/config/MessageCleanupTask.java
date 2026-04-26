package com.nova.pollink.server.config;

import com.nova.pollink.server.application.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息过期清理定时任务。
 * 定期扫描并将超过过期时间且未推送的消息标记为已超时。
 */
@Component
public class MessageCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(MessageCleanupTask.class);

    private final MessageService messageService;

    public MessageCleanupTask(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 每 60 秒执行一次过期消息清理。
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredMessages() {
        try {
            int count = messageService.cleanExpiredMessages();
            if (count > 0) {
                log.info("Cleaned {} expired messages", count);
            }
        } catch (Exception e) {
            log.error("Message cleanup failed: {}", e.getMessage());
        }
    }
}
