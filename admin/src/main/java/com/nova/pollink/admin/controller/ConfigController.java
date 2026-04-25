package com.nova.pollink.admin.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/configs")
public class ConfigController {

    private final JdbcTemplate jdbcTemplate;

    public ConfigController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> listConfigs() {
        String sql = "SELECT id, `key`, value, version, status, update_time FROM configs ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @PostMapping
    public Map<String, String> createConfig(@RequestBody Map<String, String> request) {
        String key = request.get("key");
        String value = request.get("value");
        String sql = "INSERT INTO configs (`key`, value, version, status) VALUES (?, ?, 1, 0)";
        jdbcTemplate.update(sql, key, value);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/publish")
    public Map<String, String> publishConfig(@PathVariable Long id) {
        String sql = "UPDATE configs SET status = 1, version = version + 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return Map.of("status", "ok");
    }
}
