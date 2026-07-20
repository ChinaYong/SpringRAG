package com.aiplus.spring_rag.manager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aiplus.spring_rag.dto.FileProgressEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String taskId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        SseEmitter oldEmitter = emitters.put(taskId, emitter);

        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        emitter.onCompletion(() -> emitters.remove(taskId, emitter));

        emitter.onTimeout(() -> {
            log.info("SSE 连接超时 taskId={}", taskId);
            emitters.remove(taskId, emitter);
        });

        emitter.onError(error -> {
            log.info(
                    "SSE 连接异常 taskId={}, message={}",
                    taskId,
                    error.getMessage());
            emitters.remove(taskId, emitter);
        });

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(Map.of("taskId", taskId)));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(taskId, emitter);
            throw new IllegalStateException("SSE 连接建立失败", e);
        }

        return emitter;
    }

    public void send(String taskId, FileProgressEvent event) {
        SseEmitter emitter = emitters.get(taskId);

        if (emitter == null) {
            log.debug(
                    "SSE 连接不存在 taskId={}", taskId,
                    taskId);
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("progress")
                            .data(event, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(taskId, emitter);
            log.info(
                    "SSE 消息发送失败，连接已清理 taskId={}",
                    taskId);
        }
    }

    public void complete(String taskId) {
        SseEmitter emitter = emitters.remove(taskId);

        if (emitter != null) {
            emitter.complete();
        }
    }
}
