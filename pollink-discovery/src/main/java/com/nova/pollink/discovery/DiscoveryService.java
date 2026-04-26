package com.nova.pollink.discovery;

import com.nova.pollink.discovery.dal.entity.ServerNode;
import java.util.List;

/**
 * 服务注册与发现 SPI 接口。
 * 提供节点注册、心跳、注销和节点列表查询能力。
 * 实现类通过 spring.factories 或 @ConditionalOnProperty 注入。
 * @author wentao
 */
public interface DiscoveryService {

    /**
     * 注册当前节点到注册中心。
     *
     * @param node 当前节点信息
     */
    void register(ServerNode node);

    /**
     * 发送心跳，更新节点存活时间。
     *
     * @param nodeId 节点唯一标识
     */
    void heartbeat(String nodeId);

    /**
     * 从注册中心注销节点。
     *
     * @param nodeId 节点唯一标识
     */
    void deregister(String nodeId);

    /**
     * 查询所有活跃节点列表。
     *
     * @return 活跃节点列表（已排除超时/离线的节点）
     */
    List<ServerNode> listActiveNodes();

    /**
     * 更新节点连接数统计。
     *
     * @param nodeId 节点唯一标识
     * @param connectionCount 当前连接数
     */
    void updateConnectionCount(String nodeId, int connectionCount);
}
