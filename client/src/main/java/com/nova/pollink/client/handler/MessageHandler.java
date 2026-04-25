package com.nova.pollink.client.handler;

import com.nova.pollink.client.model.Message;

/**
 * 消息处理器接口。
 * 业务方实现此接口以处理接收到的消息。
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * 处理接收到的消息。
     *
     * @param message 消息对象
     */
    void handle(Message message);
}
