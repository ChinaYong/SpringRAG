package com.aiplus.spring_rag.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.entity.UserFileInfo;
import com.aiplus.spring_rag.mapper.UserFileInfoMapper;
import com.aiplus.spring_rag.service.UserFileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserFileInfoServiceImpl extends ServiceImpl<UserFileInfoMapper, UserFileInfo>
        implements UserFileInfoService {

    @Override
    public void saveUserFileRef(Integer userId, Integer fileId) {

        boolean exists = this.lambdaQuery()
                .eq(UserFileInfo::getUserId, userId)
                .eq(UserFileInfo::getFileId, fileId)
                .exists();

        if (exists) {
            return;
        }

        try {
            UserFileInfo ufi = new UserFileInfo();
            ufi.setUserId(userId);
            ufi.setFileId(fileId);
            this.save(ufi);
        } catch (DuplicateKeyException e) {
            log.info("user_file_info 已存在 userId={}, fileId={}", userId, fileId);
        }
    }
}
