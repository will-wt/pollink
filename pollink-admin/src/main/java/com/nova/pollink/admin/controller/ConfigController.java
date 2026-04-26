package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.ConfigEntity;
import com.nova.pollink.admin.dal.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/configs")
public class ConfigController {

    private final ConfigMapper configMapper;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public ConfigController(ConfigMapper configMapper,
                            @Value("${nova.pollink.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.configMapper = configMapper;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<ConfigEntity> listConfigs() {
        return configMapper.selectAll();
    }

    @PostMapping
    public Map<String, String> createConfig(@RequestBody Map<String, String> request) {
        restTemplate.postForObject(serverUrl + "/api/v1/push/config", request, Map.class);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/publish")
    public Map<String, String> publishConfig(@PathVariable Long id) {
        configMapper.publish(id);
        return Map.of("status", "ok");
    }
}
