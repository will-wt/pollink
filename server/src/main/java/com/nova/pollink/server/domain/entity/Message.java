package com.nova.pollink.server.domain.entity;

import java.time.LocalDateTime;

/**
 * 消息领域实体。
 * 封装消息的核心属性和业务规则（如判断是否过期、是否已推送）。
 * @author wentao
 */
public class Message {

    private Long id;
    private LocalDateTime createTime;
    private String topic;
    private String clientFilter;
    private String payload;
    private int status;
    private LocalDateTime expireTime;

    public Message() {}

    /**
     * 判断消息是否已过期。
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 判断消息是否待推送。
     */
    public boolean isPending() {
        return status == 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getClientFilter() { return clientFilter; }
    public void setClientFilter(String clientFilter) { this.clientFilter = clientFilter; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
