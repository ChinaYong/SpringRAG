package com.aiplus.spring_rag.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aiplus.spring_rag.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

}
