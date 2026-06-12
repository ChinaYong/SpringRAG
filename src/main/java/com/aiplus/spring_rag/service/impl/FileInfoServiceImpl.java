package com.aiplus.spring_rag.service.impl;

import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.FileInfo;
import com.aiplus.spring_rag.mapper.FileInfoMapper;
import com.aiplus.spring_rag.service.FileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {
    
}
