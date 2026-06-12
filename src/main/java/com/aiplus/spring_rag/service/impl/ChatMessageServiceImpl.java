package com.aiplus.spring_rag.service.impl;

import com.aiplus.spring_rag.mapper.ChatMessageMapper;
import com.aiplus.spring_rag.service.ChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.ChatMessage;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService{
    
}
