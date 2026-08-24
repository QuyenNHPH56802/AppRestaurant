package com.restaurant.server.controller;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.storage.LocalFileStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUploadController {

    private final LocalFileStorage storage;

    public AdminUploadController(LocalFileStorage storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/food-image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AdminDtos.UploadResponse>> uploadFoodImage(
            @RequestParam("file") MultipartFile file) {
        var stored = storage.saveFoodImage(file);
        return ResponseEntity.ok(ApiResponse.ok(
                new AdminDtos.UploadResponse(stored.imageUrl(), stored.size(), stored.contentType())));
    }
}