package com.restaurant.server.controller;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.AdminFoodService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/foods")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodController {

    private final AdminFoodService service;

    public AdminFoodController(AdminFoodService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContentDtos.FoodView>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> create(
            @Valid @RequestBody AdminDtos.FoodRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(req, principal == null ? null : principal.id())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.FoodRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req, principal == null ? null : principal.id())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> patch(
            @PathVariable Long id,
            @RequestBody AdminDtos.FoodPatchRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.patch(id, req, principal == null ? null : principal.id())));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> setStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        String status = body.getOrDefault("status", "AVAILABLE");
        return ResponseEntity.ok(ApiResponse.ok(service.setStatus(id, status, principal == null ? null : principal.id())));
    }

    @PatchMapping("/{id}/featured")
    public ResponseEntity<ApiResponse<ContentDtos.FoodView>> setFeatured(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        boolean featured = Boolean.TRUE.equals(body.getOrDefault("isFeatured", false));
        return ResponseEntity.ok(ApiResponse.ok(service.setFeatured(id, featured, principal == null ? null : principal.id())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        service.delete(id, principal == null ? null : principal.id());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}