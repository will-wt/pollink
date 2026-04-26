package com.nova.pollink.discovery.dal.mapper;

import com.nova.pollink.discovery.dal.entity.ServerNode;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ServerNodeMapper {

    @Insert("""
        INSERT INTO server_nodes (id, ip, status, last_heartbeat, connection_count, create_time)
        VALUES (#{id}, #{ip}, #{status}, #{lastHeartbeat}, #{connectionCount}, #{createTime})
        ON DUPLICATE KEY UPDATE
            status = VALUES(status),
            last_heartbeat = VALUES(last_heartbeat),
            connection_count = VALUES(connection_count)
        """)
    void insertOrUpdate(ServerNode node);

    @Update("UPDATE server_nodes SET last_heartbeat = #{heartbeatTime} WHERE id = #{nodeId}")
    void updateHeartbeat(@Param("nodeId") String nodeId, @Param("heartbeatTime") LocalDateTime heartbeatTime);

    @Update("UPDATE server_nodes SET status = #{status} WHERE id = #{nodeId}")
    void updateStatus(@Param("nodeId") String nodeId, @Param("status") int status);

    @Select("""
        SELECT id, ip, status, last_heartbeat, connection_count, create_time
        FROM server_nodes
        WHERE status = 1 AND last_heartbeat > #{threshold}
        """)
    List<ServerNode> selectActiveNodes(@Param("threshold") LocalDateTime threshold);

    @Update("UPDATE server_nodes SET connection_count = #{count} WHERE id = #{nodeId}")
    void updateConnectionCount(@Param("nodeId") String nodeId, @Param("count") int count);
}
