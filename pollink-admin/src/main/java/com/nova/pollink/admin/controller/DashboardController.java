package com.nova.pollink.admin.controller;

import com.nova.pollink.discovery.DiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 概览仪表盘控制器。
 * 提供节点统计和系统概览数据。
 * @author wentao
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

    private final DiscoveryService discoveryService;

    public DashboardController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        var nodes = discoveryService.listActiveNodes();
        int totalConnections = nodes.stream().mapToInt(n -> n.getConnectionCount()).sum();

        return Map.of(
            "totalNodes", nodes.size(),
            "totalConnections", totalConnections
        );
    }
}
