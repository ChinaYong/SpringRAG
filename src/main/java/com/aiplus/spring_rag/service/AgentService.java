package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.entity.Agent;
import com.baomidou.mybatisplus.extension.service.IService;


public interface AgentService extends IService<Agent> {  
    /**
     * 定义一个接口，继承了 IService，其中 IService 指示了接口应该实现的方法，
     * 而泛型传入 Agent 实体对象则指示 AgentService 操作的对象是 Agent。
     */
}
