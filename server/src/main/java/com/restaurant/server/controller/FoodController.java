package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.Food;
import com.restaurant.server.service.FoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentDtos.PagedResponse<ContentDtos.FoodView>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Food.Status status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;
        return ResponseEntity.ok(ApiResponse.ok(service.list(q, categoryId, status, featured, page, size)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ContentDtos.FoodView>>> featured(
            @RequestParam(defaultValue = "10") int limit) {
        if (limit < 1) limit = 1;
        if (limit > 50) limit = 50;
        return ResponseEntity.ok(ApiResponse.ok(service.featured(limit)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }
}