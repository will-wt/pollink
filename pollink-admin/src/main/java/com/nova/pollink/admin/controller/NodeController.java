package com.nova.pollink.admin.controller;

import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.model.ServerNode;
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

    public NodeController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public List<ServerNode> listNodes() {
        return discoveryService.listActiveNodes();
    }

    @PostMapping("/{nodeId}/maintenance")
    public Map<String, String> setMaintenance(@PathVariable String nodeId) {
        // 实际实现中需要更新 server_nodes 表 status = 2
        return Map.of("status", "ok", "message", "Node " + nodeId + " set to maintenance");
    }
}
