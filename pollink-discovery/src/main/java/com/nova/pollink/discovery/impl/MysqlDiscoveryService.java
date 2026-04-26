package com.nova.pollink.discovery.impl;

import com.nova.pollink.discovery.DiscoveryProperties;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.dal.entity.ServerNode;
import com.nova.pollink.discovery.dal.mapper.ServerNodeMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "nova.pollink.discovery", name = "type", havingValue = "mysql", matchIfMissing = true)
public class MysqlDiscoveryService implements DiscoveryService {

    private final ServerNodeMapper serverNodeMapper;
    private final DiscoveryProperties properties;

    public MysqlDiscoveryService(ServerNodeMapper serverNodeMapper, DiscoveryProperties properties) {
        this.serverNodeMapper = serverNodeMapper;
        this.properties = properties;
    }

    @Override
    public void register(ServerNode node) {
        serverNodeMapper.insertOrUpdate(node);
    }

    @Override
    public void heartbeat(String nodeId) {
        serverNodeMapper.updateHeartbeat(nodeId, LocalDateTime.now());
    }

    @Override
    public void deregister(String nodeId) {
        serverNodeMapper.updateStatus(nodeId, 0);
    }

    @Override
    public List<ServerNode> listActiveNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getNodeTimeoutSeconds());
        return serverNodeMapper.selectActiveNodes(threshold);
    }

    @Override
    public void updateConnectionCount(String nodeId, int connectionCount) {
        serverNodeMapper.updateConnectionCount(nodeId, connectionCount);
    }
}
