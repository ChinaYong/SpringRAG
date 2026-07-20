package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.entity.UserFileInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserFileInfoService extends IService<UserFileInfo> {

    public void saveUserFileRef(Integer userId, Integer fileId);
}
