package com.nova.pollink.client.handler;

import com.nova.pollink.client.model.Config;

/**
 * 配置处理器接口。
 * 业务方实现此接口以处理接收到的配置变更。
 * @author wentao
 */
@FunctionalInterface
public interface ConfigHandler {

    /**
     * 处理接收到的配置变更。
     *
     * @param config 配置对象
     */
    void handle(Config config);

}
