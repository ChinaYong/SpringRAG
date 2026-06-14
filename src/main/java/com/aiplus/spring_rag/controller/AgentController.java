package com.aiplus.spring_rag.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiplus.spring_rag.common.Result;
import com.aiplus.spring_rag.dto.AgentDTO;
import com.aiplus.spring_rag.service.AgentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    
    @PostMapping("/create")
    public Result<String> createAgent(AgentDTO agentDTO, HttpServletRequest request) {
        agentService.createAgent(agentDTO, getUserIdFromRequest(request));
        return Result.success("创建成功");
    }

    @PostMapping("/update/{id}")
    public Result<String> updateAgent(@PathVariable Integer agentId, AgentDTO agentDTO, HttpServletRequest request) {
        agentService.updateAgent(agentDTO, agentId, getUserIdFromRequest(request));
        return Result.success("更新成功");
    }
    
    @PostMapping("/delete/{id}")
    public Result<String> deleteAgent(@PathVariable Integer agentId, HttpServletRequest request) {
        agentService.deleteAgent(agentId, getUserIdFromRequest(request));
        return Result.success("删除成功");
    }
    
    private Integer getUserIdFromRequest(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }
}
