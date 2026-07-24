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
import com.google.genai.types.File;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProgressService {

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
            @NonNull String status,
            @NonNull String stage,
            @NonNull Integer progress,
            @NonNull String message) {
        String progressKey = buildProgressKey(fileId);
        String tasksKey = buildTasksKey(fileId);

        FileProgressEvent event = new FileProgressEvent(
                status,
                stage,
                progress,
                message,
                LocalDateTime.now());

        // 先保存最新的文件处理进度快照
        stringRedisTemplate
                .opsForValue()
                .set(
                        progressKey,
                        toJson(event),
                        Duration.ofHours(2));

        // 查找需要广播的 task 集合
        Set<String> tasks = stringRedisTemplate.opsForSet().members(buildTasksKey(fileId));
        boolean terminal = "SUCCESS".equals(status) || "FAILED".equals(status);

        if (tasks != null) {
            for (String taskId : tasks) {
                String uploadKey = buildUploadKey(taskId);
                String uploadJson = stringRedisTemplate.opsForValue().get(uploadKey);

                if (uploadJson == null) {
                    // 清除失效的任务
                    stringRedisTemplate.opsForSet().remove(tasksKey, taskId);
                    continue;
                }

                UploadTasksEvent task = fromJson(uploadJson, UploadTasksEvent.class);
                UploadTasksEvent updatedTask = new UploadTasksEvent(
                        task.userId(),
                        fileId,
                        status,
                        stage);

                stringRedisTemplate.opsForValue().set(
                        uploadKey,
                        toJson(updatedTask),
                        Duration.ofHours(2));

                sseEmitterManager.send(taskId, event);

                if (terminal) {
                    sseEmitterManager.complete(taskId);
                }
            }
        }

        if (terminal) {
            stringRedisTemplate.delete(tasksKey);
        } else {
            stringRedisTemplate.expire(
                    tasksKey,
                    Duration.ofHours(2));
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
        String uploadJson = stringRedisTemplate.opsForValue().get(buildUploadKey(taskId));

        if (uploadJson == null) {
            throw new RuntimeException("文件处理任务不存在或已过期");
        }

        UploadTasksEvent task = fromJson(uploadJson, UploadTasksEvent.class);

        if (task.fileId() == null) {
            return new FileProgressEvent(
                    "PENDING",
                    "CREATED",
                    0,
                    "文件处理未开始",
                    LocalDateTime.now());
        }

        String progressJson = stringRedisTemplate.opsForValue().get(buildProgressKey(task.fileId()));

        if (progressJson == null) {
            return new FileProgressEvent(
                    task.status(),
                    task.stage(),
                    0,
                    "等待文件处理进度",
                    LocalDateTime.now());
        }

        return fromJson(progressJson, FileProgressEvent.class);
    }

    public void taskUserFileRef(Integer userId, Integer fileId, String taskId,
            String status, String stage) {

        UploadTasksEvent event = new UploadTasksEvent(
                userId,
                fileId,
                status,
                stage);

        stringRedisTemplate.opsForValue().set(buildUploadKey(taskId), toJson(event), Duration.ofHours(2));
    }
}
