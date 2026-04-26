package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.mapper.ServerNodeMapper;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.dal.entity.ServerNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/nodes")
public class NodeController {

    private final DiscoveryService discoveryService;
    private final ServerNodeMapper serverNodeMapper;

    public NodeController(DiscoveryService discoveryService, ServerNodeMapper serverNodeMapper) {
        this.discoveryService = discoveryService;
        this.serverNodeMapper = serverNodeMapper;
    }

    @GetMapping
    public List<ServerNode> listNodes() {
        return discoveryService.listActiveNodes();
    }

    @PostMapping("/{nodeId}/maintenance")
    public Map<String, String> setMaintenance(@PathVariable String nodeId) {
        int updated = serverNodeMapper.updateStatus(nodeId, 2);
        if (updated > 0) {
            return Map.of("status", "ok", "message", "Node " + nodeId + " set to maintenance");
        }
        return Map.of("status", "error", "message", "Node " + nodeId + " not found");
    }
}
