package com.nova.pollink.discovery.impl;

import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.dal.entity.ServerNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * DiscoveryService 的 Nacos 实现（预留，暂未实现）。
 * 当配置 nova.pollink.discovery.type=nacos 时启用。
 */
@Service
@ConditionalOnProperty(prefix = "nova.pollink.discovery", name = "type", havingValue = "nacos")
public class NacosDiscoveryService implements DiscoveryService {

    @Override
    public void register(ServerNode node) {
        throw new UnsupportedOperationException("Nacos discovery not yet implemented");
    }

    @Override
    public void heartbeat(String nodeId) {
        throw new UnsupportedOperationException("Nacos discovery not yet implemented");
    }

    @Override
    public void deregister(String nodeId) {
        throw new UnsupportedOperationException("Nacos discovery not yet implemented");
    }

    @Override
    public List<ServerNode> listActiveNodes() {
        return Collections.emptyList();
    }

    @Override
    public void updateConnectionCount(String nodeId, int connectionCount) {
        throw new UnsupportedOperationException("Nacos discovery not yet implemented");
    }
}
