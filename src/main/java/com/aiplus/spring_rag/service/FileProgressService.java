package com.aiplus.spring_rag.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.dto.FileProgressEvent;
import com.aiplus.spring_rag.dto.UploadTasksEvent;
import com.aiplus.spring_rag.manager.SseEmitterManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProgressService {

    private final UploadTasksEvent uploadTasksEvent;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // fileId 关联追踪进度
    private static final String FILE_PROGRESS_KEY_PREFIX = "file:progress:";
    // fileId 关联追踪进度的 taskId
    private static final String FILE_TASK_KEY_PREFIX = "file:tasks:";
    // taskId 关联用户Id、fileId
    private static final String UPLOAD_TASKS_KEY_PREFIX = "upload:tasks:";

    private final SseEmitterManager sseEmitterManager;

    // 上传文件进度上报与更新
    public void reportWithFile(
            @NonNull Integer fileId,
            String status,
            String stage,
            int progress,
            String message) {
        Set<String> tasks = stringRedisTemplate.opsForSet().members(buildTasksKey(fileId));

        for (String taskId : tasks) {
            // 获取任务关联的用户Id
            Integer userId = fromJson(stringRedisTemplate.opsForValue().get(buildUploadKey(taskId)),
                    UploadTasksEvent.class).userId();

            String redisProgressKey = buildProgressKey(fileId);
            String oldJson = stringRedisTemplate.opsForValue().get(redisProgressKey);

            if (oldJson == null) {
                throw new RuntimeException("文件处理任务不存在或已过期");
            }

            FileProgressEvent oldEvent = fromJson(oldJson, FileProgressEvent.class);

            if (status == null) {
                status = oldEvent.status();
            }

            if (stage == null) {
                stage = oldEvent.stage();
            }

            if (progress == 0) {
                progress = oldEvent.progress();
            }

            if (message == null) {
                message = oldEvent.message();
            }

            FileProgressEvent event = new FileProgressEvent(
                    status,
                    stage,
                    progress,
                    message,
                    LocalDateTime.now());

            stringRedisTemplate
                    .opsForValue()
                    .set(
                            redisProgressKey,
                            toJson(event),
                            Duration.ofHours(2));

            String redisUploadKey = buildUploadKey(taskId);
            UploadTasksEvent uploadTasksEvent = new UploadTasksEvent(
                    userId,
                    fileId,
                    status,
                    stage);
            stringRedisTemplate
                    .opsForValue()
                    .set(
                            redisUploadKey,
                            toJson(uploadTasksEvent),
                            Duration.ofHours(2));

            sseEmitterManager.send(taskId, event);

            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                sseEmitterManager.complete(taskId);
            }
        }
    }

    public String createTask(Integer userId) {

        for (int i = 0; i < 5; i++) {
            String taskId = UUID.randomUUID().toString();
            String redisKey = buildUploadKey(taskId);

            UploadTasksEvent event = new UploadTasksEvent(
                    userId.intValue(),
                    null,
                    "PENDING",
                    "CREATED");

            Boolean created = stringRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                            redisKey,
                            toJson(event),
                            Duration.ofHours(2));

            if (Boolean.TRUE.equals(created)) {
                return taskId;
            }
        }

        throw new RuntimeException("文件处理任务创建失败");
    }

    public <T> T fromJson(String json, Class<T> T) {
        try {
            return objectMapper.readValue(json, T);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 文件进度 JSON 解析失败", e);
        }
    }

    public <T> String toJson(T event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 处理失败", e);
        }
    }

    public String buildProgressKey(Integer fileId) {
        return FILE_PROGRESS_KEY_PREFIX + fileId;
    }

    public String buildTasksKey(Integer fileId) {
        return FILE_TASK_KEY_PREFIX + fileId;
    }

    public String buildUploadKey(String taskId) {
        return UPLOAD_TASKS_KEY_PREFIX + taskId;
    }

    public boolean judgeOwnedTask(String taskId, Integer userId) {
        String json = stringRedisTemplate.opsForValue().get(buildUploadKey(taskId));

        if (json == null) {
            throw new RuntimeException("文件处理任务不存在或已过期");
        }

        UploadTasksEvent event = fromJson(json, UploadTasksEvent.class);

        if (!event.userId().equals(userId)) {
            throw new RuntimeException("无权操作该文件处理任务");
        }

        return true;
    }

    public FileProgressEvent getLatestEvent(String taskId) {
        String json = stringRedisTemplate.opsForValue().get(buildUploadKey(taskId));
        UploadTasksEvent event = fromJson(json, UploadTasksEvent.class);
        Integer fileId = event.fileId();

        String progressJson = stringRedisTemplate.opsForValue().get(buildProgressKey(fileId));
        return fromJson(progressJson, FileProgressEvent.class);
    }
}
