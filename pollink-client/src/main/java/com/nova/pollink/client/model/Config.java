package com.nova.pollink.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 客户端配置 DTO。
 * 对应 Server 返回的配置数据结构。
 * @author wentao
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    private Long id;
    private String key;
    private String value;
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
