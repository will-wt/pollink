package com.nova.pollink.admin.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 灰度规则管理控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/gray-rules")
public class GrayRuleController {

    private final JdbcTemplate jdbcTemplate;

    public GrayRuleController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> listGrayRules() {
        String sql = "SELECT id, name, type, target_id, filter_json, status, create_time FROM gray_rules ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @PostMapping
    public Map<String, String> createGrayRule(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        int type = (Integer) request.get("type");
        long targetId = ((Number) request.get("targetId")).longValue();
        String filterJson = (String) request.get("filterJson");
        String sql = "INSERT INTO gray_rules (name, type, target_id, filter_json, status) VALUES (?, ?, ?, ?, 0)";
        jdbcTemplate.update(sql, name, type, targetId, filterJson);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/enable")
    public Map<String, String> enableGrayRule(@PathVariable Long id) {
        String sql = "UPDATE gray_rules SET status = 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/disable")
    public Map<String, String> disableGrayRule(@PathVariable Long id) {
        String sql = "UPDATE gray_rules SET status = 0 WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return Map.of("status", "ok");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteGrayRule(@PathVariable Long id) {
        String sql = "DELETE FROM gray_rules WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return Map.of("status", "ok");
    }
}
