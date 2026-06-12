package com.aiplus.spring_rag.service.impl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.Agent;
import com.aiplus.spring_rag.mapper.AgentMapper;
import com.aiplus.spring_rag.service.AgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService{
    /**
     * AgentServiceImpl 继承自 ServiceImpl 后，其中 ServiceImpl 实现了 IService 接口中的方法，
     * 而传入的 AgentMapper 让 ServiceImpl 操作 Agent 表的数据，
     * 传入的 Agent 让 ServiceImpl 操作的数据的另一方是 Agent（数据传入模型： A <-> B ，Agent <-> Agent表，
     * ServiceImpl 则是 "<->"）。
     * 尽管 ServiceImpl 实现了 IService 中的方法，但是考虑到 UserService 可能自定义方法，所以 AgentServiceImpl 需要实现 AgentService 接口
     * 额外实现 AgentService 接口。
     */
}
