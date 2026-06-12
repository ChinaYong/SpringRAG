package com.aiplus.spring_rag.service.impl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.Session;
import com.aiplus.spring_rag.mapper.SessionMapper;
import com.aiplus.spring_rag.service.SessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {
    
}
