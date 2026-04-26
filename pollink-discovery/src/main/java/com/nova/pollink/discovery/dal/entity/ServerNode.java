package com.nova.pollink.discovery.dal.entity;

import java.time.LocalDateTime;

public class ServerNode {

    private String id;
    private String ip;
    private int status;
    private LocalDateTime lastHeartbeat;
    private int connectionCount;
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
