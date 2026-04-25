package com.nova.pollink.admin.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息管理控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/messages")
public class MessageController {

    private final JdbcTemplate jdbcTemplate;

    public MessageController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        String topic = request.get("topic");
        String payload = request.get("payload");
        String sql = "INSERT INTO messages (topic, payload, status, expire_time) VALUES (?, ?, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
        jdbcTemplate.update(sql, topic, payload);
        return Map.of("status", "ok");
    }
}
