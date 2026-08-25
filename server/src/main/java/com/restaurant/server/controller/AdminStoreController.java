package com.restaurant.server.controller;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.AdminStoreService;
import com.restaurant.server.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/store")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final AdminStoreService service;
    private final StoreService storeService;

    public AdminStoreController(AdminStoreService service, StoreService storeService) {
        this.service = service;
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentDtos.StoreView>> get() {
        return ResponseEntity.ok(ApiResponse.ok(storeService.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ContentDtos.StoreView>> upsert(
            @Valid @RequestBody AdminDtos.StoreRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.upsert(req, principal == null ? null : principal.id())));
    }
}