package com.aiplus.spring_rag.dto;

public record UploadTasksEvent(
        Integer userId,
        Integer fileId,
        String status,
        String stage) {

}
