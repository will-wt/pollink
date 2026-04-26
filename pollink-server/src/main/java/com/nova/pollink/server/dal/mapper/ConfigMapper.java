package com.nova.pollink.server.dal.mapper;

import com.nova.pollink.server.dal.entity.Config;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 配置 MyBatis Mapper。
 */
@Mapper
public interface ConfigMapper {

    @Insert("""
        INSERT INTO configs (`key`, value, version, client_filter, status)
        VALUES (#{key}, #{value}, #{version}, #{clientFilter}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Config config);

    @Select("""
        SELECT id, create_time, `key`, value, version, client_filter, status, update_time
        FROM configs WHERE `key` = #{key}
        """)
    Optional<Config> selectByKey(String key);

    @Select("""
        SELECT id, create_time, `key`, value, version, client_filter, status, update_time
        FROM configs
        WHERE status = 1 AND version > #{version}
        ORDER BY version
        """)
    List<Config> selectPublishedAfterVersion(int version);

    @Update("UPDATE configs SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE configs SET version = version + 1 WHERE id = #{id}")
    void incrementVersion(Long id);
}
