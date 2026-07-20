package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.dto.AgentDTO;
import com.aiplus.spring_rag.entity.Agent;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AgentService extends IService<Agent> {
    /**
     * 定义一个接口，继承了 IService，其中 IService 指示了接口应该实现的方法，
     * 而泛型传入 Agent 实体对象则指示 AgentService 操作的对象是 Agent。
     */

    void createAgent(AgentDTO agentDTO, Integer userId);

    void updateAgent(AgentDTO agentDTO, Integer agentId, Integer userId);

    void deleteAgent(Integer agentId, Integer userId);

    /**
     * 向指定 agent 的 file_ids(JSON 数组) 追加一个 file_id（去重、原子）。
     * 同时校验 agent 归属：仅当 agent 属于该 userId 时才更新。
     * 
     * @param userId  当前登录用户ID（用于归属校验，来自 JWT）
     * @param agentId 智能体ID
     * @param fileId  file_storage.id
     */
    void appendFileId(Integer userId, Integer agentId, Integer fileId);
}
