package com.security.controller;

import java.nio.file.Path;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.security.service.S3UploadService;

@RestController
@RequestMapping("/test")
public class TestUploadController {

    private final S3UploadService uploadService;

    public TestUploadController(S3UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload-jose")
    public String upload() throws Exception {
       // uploadService.uploadJosePng(Path.of("/mnt/c/Java/jose.png"));
        return "Upload feito com sucesso";
    }
}