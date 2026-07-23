package com.aiplus.spring_rag.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aiplus.spring_rag.dto.FileProgressEvent;
import com.aiplus.spring_rag.manager.SseEmitterManager;
import com.aiplus.spring_rag.service.FileProgressService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("api/file-progress")
@RequiredArgsConstructor
public class FileProgressController {

    private final FileProgressService fileProgressService;
    private final SseEmitterManager sseEmitterManager;

    @PostMapping("/tasks")
    public Map<String, String> createTask(@RequestAttribute("userId") Integer Id) {
        String taskId = fileProgressService.createTask(Id);

        Map<String, String> entity = Map.of("taskId", taskId);
        return entity;
    }

    @GetMapping(path = "/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String taskId, @RequestAttribute("userId") Integer userId) {
        fileProgressService.judgeOwnedTask(taskId, userId);
        SseEmitter emitter = sseEmitterManager.register(taskId);

        /**
         * 之所以调用 subscribe 方法，是因为第一次建立连接，或者前台刷新需要重新
         * 建立连接。
         * 且考虑到后台文件处理过程与 SSE 连接是异步的，所以文件处理中调用 report 方法时可能
         * 此时SSE 连接不存在（没有建立连接或前台刷新），所以需要在重新注册后，再次发送一次事件
         * 到前台，以避免进度丢失（尤其是已完成的进度）
         */
        try {
            FileProgressEvent latestEvent = fileProgressService.getLatestEvent(taskId);

            sseEmitterManager.send(taskId, latestEvent);

            if (isTerminal(latestEvent.status())) {
                sseEmitterManager.complete(taskId);
            }

            return emitter;
        } catch (RuntimeException e) {
            sseEmitterManager.complete(taskId);
            throw e;
        }
    }

    public boolean isTerminal(String status) {
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            return true;
        } else {
            return false;
        }
    }
}
