package com.nova.pollink.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器。
 * 查询操作直接访问数据库，写入操作通过 HTTP 调用 server 的 push 接口，
 * 以触发 gRPC 集群广播和客户端唤醒。
 */
@RestController
@RequestMapping("/api/v1/admin/configs")
public class ConfigController {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public ConfigController(JdbcTemplate jdbcTemplate,
                            @Value("${nova.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<Map<String, Object>> listConfigs() {
        String sql = "SELECT id, `key`, value, version, status, update_time FROM configs ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @PostMapping
    public Map<String, String> createConfig(@RequestBody Map<String, String> request) {
        // 通过 server 的 push 接口创建并发布配置，确保触发 gRPC 集群广播
        restTemplate.postForObject(serverUrl + "/api/v1/push/config", request, Map.class);
        return Map.of("status", "ok");
    }
}
