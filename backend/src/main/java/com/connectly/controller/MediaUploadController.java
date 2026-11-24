package com.connectly.controller;

import com.connectly.service.MediaStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private final MediaStorageService mediaStorageService;

    public MediaUploadController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaStorageService.storeFile(file);
        Map<String, Object> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("name", file.getOriginalFilename());
        response.put("size", file.getSize());
        response.put("contentType", file.getContentType());
        return ResponseEntity.ok(response);
    }
}
