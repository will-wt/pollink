package com.nova.pollink.discovery.impl;

import com.nova.pollink.discovery.DiscoveryProperties;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.model.ServerNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DiscoveryService 的 MySQL 实现（默认）。
 * 使用 JDBC Template 直接操作 server_nodes 表，不引入 ORM 依赖，保持 discovery 模块轻量。
 * @author wentao
 */
@Service
@ConditionalOnProperty(prefix = "nova.pollink.discovery", name = "type", havingValue = "mysql", matchIfMissing = true)
public class MysqlDiscoveryService implements DiscoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final DiscoveryProperties properties;

    public MysqlDiscoveryService(JdbcTemplate jdbcTemplate, DiscoveryProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void register(ServerNode node) {
        String sql = """
            INSERT INTO server_nodes (id, ip, status, last_heartbeat, connection_count, create_time)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                status = VALUES(status),
                last_heartbeat = VALUES(last_heartbeat),
                connection_count = VALUES(connection_count)
            """;
        jdbcTemplate.update(sql, node.getId(), node.getIp(), node.getStatus(),
            node.getLastHeartbeat(), node.getConnectionCount(), node.getCreateTime());
    }

    @Override
    public void heartbeat(String nodeId) {
        String sql = "UPDATE server_nodes SET last_heartbeat = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), nodeId);
    }

    @Override
    public void deregister(String nodeId) {
        String sql = "UPDATE server_nodes SET status = 0 WHERE id = ?";
        jdbcTemplate.update(sql, nodeId);
    }

    @Override
    public List<ServerNode> listActiveNodes() {
        String sql = """
            SELECT id, ip, status, last_heartbeat, connection_count, create_time
            FROM server_nodes
            WHERE status = 1 AND last_heartbeat > ?
            """;
        LocalDateTime threshold = LocalDateTime.now()
            .minusSeconds(properties.getNodeTimeoutSeconds());
        return jdbcTemplate.query(sql, new ServerNodeMapper(), threshold);
    }

    @Override
    public void updateConnectionCount(String nodeId, int connectionCount) {
        String sql = "UPDATE server_nodes SET connection_count = ? WHERE id = ?";
        jdbcTemplate.update(sql, connectionCount, nodeId);
    }

    private static class ServerNodeMapper implements RowMapper<ServerNode> {
        @Override
        public ServerNode mapRow(ResultSet rs, int rowNum) throws SQLException {
            ServerNode node = new ServerNode();
            node.setId(rs.getString("id"));
            node.setIp(rs.getString("ip"));
            node.setStatus(rs.getInt("status"));
            node.setLastHeartbeat(rs.getTimestamp("last_heartbeat").toLocalDateTime());
            node.setConnectionCount(rs.getInt("connection_count"));
            node.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            return node;
        }
    }
}
