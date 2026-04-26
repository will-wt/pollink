package com.nova.pollink.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 客户端消息 DTO。
 * 对应 Server 返回的消息数据结构。
 * @author wentao
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private Long id;
    private String topic;
    private String payload;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
