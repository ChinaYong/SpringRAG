package com.aiplus.spring_rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aiplus.spring_rag.entity.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    /**
     * 向 agent.file_ids(JSON 数组) 原子追加一个 file_id。
     * <p>
     * 使用 MySQL JSON 函数，单条 SQL 完成「读-改-写」，天然避免并发丢更新：
     * <ul>
     * <li>IFNULL(file_ids, JSON_ARRAY()) 兼容首次写入时字段为 NULL；</li>
     * <li>JSON_ARRAY_APPEND 在 '$' 末尾追加元素；</li>
     * <li>NOT JSON_CONTAINS 保证去重，已存在则不追加。</li>
     * </ul>
     * 
     * @return 1=新增成功；0=该 fileId 已存在、或 agent 不存在/不属于该用户
     */
    @Update("UPDATE agent SET file_ids = JSON_ARRAY_APPEND(IFNULL(file_ids, JSON_ARRAY()), '$', #{fileId}), "
            + "update_time = NOW() WHERE id = #{agentId} AND user_id = #{userId} "
            + "AND NOT JSON_CONTAINS(IFNULL(file_ids, JSON_ARRAY()), CAST(#{fileId} AS JSON))")
    int appendFileId(@Param("userId") Integer userId, @Param("agentId") Integer agentId,
            @Param("fileId") Integer fileId);
}
