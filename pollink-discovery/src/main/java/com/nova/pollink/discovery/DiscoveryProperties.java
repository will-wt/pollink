package com.nova.pollink.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discovery 模块的配置属性。
 * @author wentao
 */
@ConfigurationProperties(prefix = "nova.pollink.discovery")
public class DiscoveryProperties {

    /** 注册中心类型：mysql（默认）或 nacos */
    private String type = "mysql";

    /** 心跳间隔（秒） */
    private int heartbeatIntervalSeconds = 5;

    /** 节点超时时间（秒），超过此时间未收到心跳视为离线 */
    private int nodeTimeoutSeconds = 15;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getHeartbeatIntervalSeconds() { return heartbeatIntervalSeconds; }
    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) { this.heartbeatIntervalSeconds = heartbeatIntervalSeconds; }

    public int getNodeTimeoutSeconds() { return nodeTimeoutSeconds; }
    public void setNodeTimeoutSeconds(int nodeTimeoutSeconds) { this.nodeTimeoutSeconds = nodeTimeoutSeconds; }
}
