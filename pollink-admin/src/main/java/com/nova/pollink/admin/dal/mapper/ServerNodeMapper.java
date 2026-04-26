package com.nova.pollink.admin.dal.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ServerNodeMapper {

    @Update("UPDATE server_nodes SET status = #{status} WHERE id = #{nodeId}")
    int updateStatus(@Param("nodeId") String nodeId, @Param("status") int status);
}
