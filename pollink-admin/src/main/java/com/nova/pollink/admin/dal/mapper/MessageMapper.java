package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.MessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("""
        SELECT id, topic, payload, status, create_time, expire_time
        FROM messages ORDER BY id DESC LIMIT #{limit}
        """)
    List<MessageEntity> selectRecent(@Param("limit") int limit);

    @Insert("""
        INSERT INTO messages (topic, payload, status, expire_time)
        VALUES (#{topic}, #{payload}, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE))
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MessageEntity message);
}
