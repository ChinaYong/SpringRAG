package com.aiplus.spring_rag.service.impl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.dto.AgentDTO;
import com.aiplus.spring_rag.entity.Agent;
import com.aiplus.spring_rag.mapper.AgentMapper;
import com.aiplus.spring_rag.service.AgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {
    /**
     * AgentServiceImpl 继承自 ServiceImpl 后，其中 ServiceImpl 实现了 IService 接口中的方法，
     * 而传入的 AgentMapper 让 ServiceImpl 操作 Agent 表的数据，
     * 传入的 Agent 让 ServiceImpl 操作的数据的另一方是 Agent（数据传入模型： A <-> B ，Agent <-> Agent表，
     * ServiceImpl 则是 "<->"）。
     * 尽管 ServiceImpl 实现了 IService 中的方法，但是考虑到 UserService 可能自定义方法，所以
     * AgentServiceImpl 需要实现 AgentService 接口
     * 额外实现 AgentService 接口。
     */

    @Override
    public void createAgent(AgentDTO agentDTO, Integer userId) {
        if (this.lambdaQuery().eq(Agent::getUserId, userId).eq(Agent::getName, agentDTO.getName()).one() != null) {
            throw new RuntimeException("不允许创建重名 Agent ！");
        }
        Agent agent = new Agent(agentDTO);
        agent.setUserId(userId);
        this.save(agent);
    }

    @Override
    public void updateAgent(AgentDTO agentDTO, Integer agentId, Integer userId) {
        Agent agent = new Agent(agentDTO);

        boolean success = this.lambdaUpdate()
                .eq(Agent::getId, agentId)
                .eq(Agent::getUserId, userId)
                .update(agent);

        if (!success) {
            throw new RuntimeException("更新失败！");
        }
    }

    @Override
    public void deleteAgent(Integer agentId, Integer userId) {

        boolean success = this.lambdaUpdate()
                .eq(Agent::getId, agentId)
                .eq(Agent::getUserId, userId)
                .remove();

        if (!success) {
            throw new RuntimeException("删除失败！");
        }
    }

    @Override
    public void appendFileId(Integer userId, Integer agentId, Integer fileId) {
        // 原子追加 + 归属校验 + 去重，单条 SQL 完成。
        // 返回 1=新增成功；0=可能重复、或 agent 不存在/不属于该用户（越权），记日志便于排查但不阻断流程
        int rows = baseMapper.appendFileId(userId, agentId, fileId);
        if (rows == 0) {
            log.warn("appendFileId 未生效: userId={}, agentId={}, fileId={}（可能重复，或 agent 不存在/不属于该用户）",
                    userId, agentId, fileId);
        }
    }
}
