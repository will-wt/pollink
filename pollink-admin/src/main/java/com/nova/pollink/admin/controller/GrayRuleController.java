package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.GrayRuleEntity;
import com.nova.pollink.admin.dal.mapper.GrayRuleMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/gray-rules")
public class GrayRuleController {

    private final GrayRuleMapper grayRuleMapper;

    public GrayRuleController(GrayRuleMapper grayRuleMapper) {
        this.grayRuleMapper = grayRuleMapper;
    }

    @GetMapping
    public List<GrayRuleEntity> listGrayRules() {
        return grayRuleMapper.selectAll();
    }

    @PostMapping
    public Map<String, String> createGrayRule(@RequestBody Map<String, Object> request) {
        GrayRuleEntity rule = new GrayRuleEntity();
        rule.setName((String) request.get("name"));
        rule.setType((Integer) request.get("type"));
        rule.setTargetId(((Number) request.get("targetId")).longValue());
        rule.setFilterJson((String) request.get("filterJson"));
        grayRuleMapper.insert(rule);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/enable")
    public Map<String, String> enableGrayRule(@PathVariable Long id) {
        grayRuleMapper.enable(id);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/disable")
    public Map<String, String> disableGrayRule(@PathVariable Long id) {
        grayRuleMapper.disable(id);
        return Map.of("status", "ok");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteGrayRule(@PathVariable Long id) {
        grayRuleMapper.delete(id);
        return Map.of("status", "ok");
    }
}
