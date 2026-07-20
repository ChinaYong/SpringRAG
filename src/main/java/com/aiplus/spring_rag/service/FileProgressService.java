package com.aiplus.spring_rag.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.dto.FileProgressEvent;
import com.aiplus.spring_rag.manager.SseEmitterManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProgressService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "file:progress:";
    private final SseEmitterManager sseEmitterManager;

    public void report(
            String taskId,
            Integer userId,
            Integer fileId,
            String status,
            String stage,
            int progress,
            String message) {
        String redisKey = buildKey(taskId);

        String oldJson = stringRedisTemplate.opsForValue().get(redisKey);

        if (oldJson == null) {
            throw new RuntimeException("文件处理任务不存在或已过期");
        }

        FileProgressEvent oldEvent = fromJson(oldJson);

        if (!oldEvent.userId().equals(userId)) {
            throw new RuntimeException("无权操作该文件处理任务");
        }

        if (oldEvent.fileId() != null
                && !oldEvent.fileId().equals(fileId)) {
            throw new RuntimeException("该任务已经绑定文件，不能重复绑定");
        }

        if (fileId == null) {
            fileId = oldEvent.fileId();
        }

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
                taskId,
                userId.intValue(),
                fileId,
                status,
                stage,
                progress,
                message,
                LocalDateTime.now());

        stringRedisTemplate
                .opsForValue()
                .set(
                        redisKey,
                        toJson(event),
                        Duration.ofHours(2));

        sseEmitterManager.send(taskId, event);

        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            sseEmitterManager.complete(taskId);
        }
    }

    public String createTask(Integer userId) {

        for (int i = 0; i < 5; i++) {
            String taskId = UUID.randomUUID().toString();
            String redisKey = buildKey(taskId);

            FileProgressEvent event = new FileProgressEvent(
                    taskId,
                    userId.intValue(),
                    null,
                    "PENDING",
                    "CREATED",
                    0,
                    "文件处理任务已创建",
                    LocalDateTime.now());

            String json = toJson(event);

            Boolean created = stringRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                            redisKey,
                            json,
                            Duration.ofHours(2));

            if (Boolean.TRUE.equals(created)) {
                return taskId;
            }
        }

        throw new RuntimeException("文件处理任务创建失败");
    }

    private FileProgressEvent fromJson(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    FileProgressEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 文件进度 JSON 解析失败", e);
        }
    }

    private String toJson(FileProgressEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 处理失败", e);
        }
    }

    private String buildKey(String taskId) {
        return KEY_PREFIX + taskId;
    }

    /**
     * 将生成的文件 ID 绑定到 Task
     * TODO： 考虑删除
     */
    public FileProgressEvent bindFile(Integer userId, String taskId, Integer fileId) {
        String redisKey = buildKey(taskId);

        String json = stringRedisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            throw new RuntimeException("文件处理任务不存在或已过期");
        }

        FileProgressEvent oldEvent = fromJson(json);

        if (!oldEvent.userId().equals(userId)) {
            throw new RuntimeException("无权操作该文件处理任务");
        }

        if (oldEvent.fileId() != null) {
            throw new RuntimeException("该任务已经绑定文件，不能重复绑定");
        }

        FileProgressEvent event = new FileProgressEvent(
                oldEvent.taskId(),
                oldEvent.userId(),
                fileId,
                "PROCESSING",
                "STORED",
                15,
                "文件保存完成，准备解析",
                LocalDateTime.now());

        stringRedisTemplate.opsForValue().set(redisKey, toJson(event), Duration.ofHours(2));

        return event;
    }

    public FileProgressEvent getOwnedTask(String taskId, Integer userId) {
        String json = stringRedisTemplate.opsForValue().get(buildKey(taskId));

        if (json == null) {
            throw new RuntimeException("文件处理任务不存在或已过期");
        }

        FileProgressEvent event = fromJson(json);

        if (!event.userId().equals(userId)) {
            throw new RuntimeException("无权操作该文件处理任务");
        }

        return event;
    }
}
