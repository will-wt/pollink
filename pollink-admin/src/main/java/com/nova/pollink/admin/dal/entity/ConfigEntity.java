package com.nova.pollink.admin.dal.entity;

import java.time.LocalDateTime;

public class ConfigEntity {
    private Long id;
    private String key;
    private String value;
    private int version;
    private int status;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
