package com.aiplus.spring_rag.service.impl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.FileStorage;
import com.aiplus.spring_rag.mapper.FileStorageMapper;
import com.aiplus.spring_rag.service.FileStorageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class FileStorageServiceImpl extends  ServiceImpl<FileStorageMapper, FileStorage> implements FileStorageService {
    
}
