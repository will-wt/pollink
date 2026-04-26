package com.nova.pollink.server.dal.mapper;

import com.nova.pollink.server.dal.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息 MyBatis Mapper。
 */
@Mapper
public interface MessageMapper {

    @Insert("""
        INSERT INTO messages (topic, client_filter, payload, status, expire_time)
        VALUES (#{topic}, #{clientFilter}, #{payload}, #{status}, #{expireTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Message message);

    @Select("""
        SELECT id, create_time, topic, client_filter, payload, status, expire_time
        FROM messages
        WHERE topic = #{topic} AND status = 0 AND id > #{lastId}
        ORDER BY id
        LIMIT #{limit}
        """)
    List<Message> selectPendingByTopic(@Param("topic") String topic,
                                       @Param("lastId") Long lastId,
                                       @Param("limit") int limit);

    @Update("UPDATE messages SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE messages SET status = 2 WHERE status = 0 AND expire_time < NOW()")
    int cleanExpired();
}
