package com.nova.pollink.admin.controller;

import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.model.ServerNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 节点管理控制器。
 * @author wentao
 */
@RestController
@RequestMapping("/api/v1/admin/nodes")
public class NodeController {

    private final DiscoveryService discoveryService;
    private final JdbcTemplate jdbcTemplate;

    public NodeController(DiscoveryService discoveryService, JdbcTemplate jdbcTemplate) {
        this.discoveryService = discoveryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<ServerNode> listNodes() {
        return discoveryService.listActiveNodes();
    }

    @PostMapping("/{nodeId}/maintenance")
    public Map<String, String> setMaintenance(@PathVariable String nodeId) {
        String sql = "UPDATE server_nodes SET status = 2 WHERE id = ?";
        int updated = jdbcTemplate.update(sql, nodeId);
        if (updated > 0) {
            return Map.of("status", "ok", "message", "Node " + nodeId + " set to maintenance");
        }
        return Map.of("status", "error", "message", "Node " + nodeId + " not found");
    }
}
