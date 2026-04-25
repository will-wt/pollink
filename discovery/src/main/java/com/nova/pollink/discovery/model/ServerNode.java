package com.nova.pollink.discovery.model;

import java.time.LocalDateTime;

/**
 * Server 节点信息。
 * 用于注册中心记录各节点的 IP、状态、心跳和连接数。
 */
public class ServerNode {

    /** 节点唯一标识，格式为 ip */
    private String id;

    /** 节点 IP 地址 */
    private String ip;

    /** 节点状态：0=离线, 1=在线, 2=维护中 */
    private int status;

    /** 上次心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 当前活跃连接数 */
    private int connectionCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    public ServerNode() {}

    public ServerNode(String id, String ip) {
        this.id = id;
        this.ip = ip;
        this.status = 1;
        this.connectionCount = 0;
        this.lastHeartbeat = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public int getConnectionCount() { return connectionCount; }
    public void setConnectionCount(int connectionCount) { this.connectionCount = connectionCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
