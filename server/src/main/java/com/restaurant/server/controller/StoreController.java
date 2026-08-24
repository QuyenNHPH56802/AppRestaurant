package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentDtos.StoreView>> get() {
        return ResponseEntity.ok(ApiResponse.ok(service.get()));
    }
}