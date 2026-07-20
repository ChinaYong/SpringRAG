package com.aiplus.spring_rag.dto;

import java.time.LocalDateTime;

public record FileProgressEvent(
        // 一次文件处理任务的唯一编号
        String taskId,

        // 任务所属用户，用来防止用户查看别人的任务
        Integer userId,

        // 文件保存成功后产生的文件 ID，保存前可以为空
        Integer fileId,

        // 总体状态：PENDING、PROCESSING、SUCCESS、FAILED
        String status,

        // 当前阶段：CREATED、STORED、PARSING、SPLITTING、EMBEDDING、COMPLETED
        String stage,

        // 当前进度，范围为 0～100
        int progress,

        // 给前端用户看的提示信息
        String message,

        // 本次状态更新时间
        LocalDateTime updatedAt) {
}
