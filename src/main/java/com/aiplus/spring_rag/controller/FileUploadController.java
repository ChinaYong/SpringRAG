package com.aiplus.spring_rag.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.aiplus.spring_rag.dto.FileUploadDTO;
import com.aiplus.spring_rag.dto.FileUploadResponseDTO;
import com.aiplus.spring_rag.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    @PostMapping(value = "/api/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponseDTO uploadFile(@RequestAttribute("userId") Integer userId,
            @ModelAttribute FileUploadDTO fileUploadDTO) {
        fileUploadDTO.setUserId(userId);
        return fileService.uploadFileHandle(fileUploadDTO);
    }
}
