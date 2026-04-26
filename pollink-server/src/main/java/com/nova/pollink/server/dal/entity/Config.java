package com.nova.pollink.server.dal.entity;

import java.time.LocalDateTime;

/**
 * 配置领域实体。
 * 封装配置的核心属性，支持版本号用于客户端增量同步。
 * @author wentao
 */
public class Config {

    private Long id;
    private LocalDateTime createTime;
    private String key;
    private String value;
    private int version;
    private String clientFilter;
    private int status;
    private LocalDateTime updateTime;

    public Config() {}

    /**
     * 判断配置是否已发布。
     */
    public boolean isPublished() {
        return status == 1;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getClientFilter() { return clientFilter; }
    public void setClientFilter(String clientFilter) { this.clientFilter = clientFilter; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
