package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.GrayRuleEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GrayRuleMapper {

    @Select("""
        SELECT id, name, type, target_id, filter_json, status, create_time
        FROM gray_rules ORDER BY id DESC
        """)
    List<GrayRuleEntity> selectAll();

    @Insert("""
        INSERT INTO gray_rules (name, type, target_id, filter_json, status)
        VALUES (#{name}, #{type}, #{targetId}, #{filterJson}, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GrayRuleEntity rule);

    @Update("UPDATE gray_rules SET status = 1 WHERE id = #{id}")
    void enable(@Param("id") Long id);

    @Update("UPDATE gray_rules SET status = 0 WHERE id = #{id}")
    void disable(@Param("id") Long id);

    @Delete("DELETE FROM gray_rules WHERE id = #{id}")
    void delete(@Param("id") Long id);
}
