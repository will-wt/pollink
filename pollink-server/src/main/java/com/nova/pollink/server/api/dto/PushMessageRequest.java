package com.nova.pollink.server.api.dto;

/**
 * 推送消息请求 DTO。
 */
public class PushMessageRequest {

    private String topic;
    private String payload;
    private int expireSeconds = 300;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(int expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
