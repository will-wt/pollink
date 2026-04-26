package com.nova.pollink.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 消息管理控制器。
 * 查询操作直接访问数据库，写入操作通过 HTTP 调用 server 的 push 接口，
 * 以触发 gRPC 集群广播和客户端唤醒。
 */
@RestController
@RequestMapping("/api/v1/admin/messages")
public class MessageController {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public MessageController(JdbcTemplate jdbcTemplate,
                             @Value("${nova.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<Map<String, Object>> listMessages(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        String sql = "SELECT id, topic, payload, status, create_time FROM messages ORDER BY id DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }

    @PostMapping("/send")
    public Map<String, String> sendTestMessage(@RequestBody Map<String, String> request) {
        // 通过 server 的 push 接口发送消息，确保触发 gRPC 集群广播
        restTemplate.postForObject(serverUrl + "/api/v1/push/message", request, Map.class);
        return Map.of("status", "ok");
    }
}
