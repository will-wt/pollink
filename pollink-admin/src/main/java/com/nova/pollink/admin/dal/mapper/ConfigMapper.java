package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.ConfigEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConfigMapper {

    @Select("""
        SELECT id, `key`, value, version, status, update_time
        FROM configs ORDER BY id DESC
        """)
    List<ConfigEntity> selectAll();

    @Insert("""
        INSERT INTO configs (`key`, value, version, status)
        VALUES (#{key}, #{value}, 1, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ConfigEntity config);

    @Update("UPDATE configs SET status = 1, version = version + 1 WHERE id = #{id}")
    void publish(@Param("id") Long id);
}
