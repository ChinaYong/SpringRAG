package com.aiplus.spring_rag.dto;

import java.time.LocalDateTime;

public record FileProgressEvent(

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
