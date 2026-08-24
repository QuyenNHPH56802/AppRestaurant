package com.restaurant.server.controller;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminDtos.UserView>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminDtos.UserView>> create(
            @Valid @RequestBody AdminDtos.UserRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(req, principal == null ? null : principal.id())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDtos.UserView>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.UserRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req, principal == null ? null : principal.id())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDtos.UserView>> patch(
            @PathVariable Long id,
            @RequestBody AdminDtos.UserPatchRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.patch(id, req, principal == null ? null : principal.id())));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminDtos.UserView>> setStatus(
            @PathVariable Long id,
            @RequestBody AdminDtos.UserPatchRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.patch(id, req, principal == null ? null : principal.id())));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<AdminDtos.UserView>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.UserResetPasswordRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.resetPassword(id, req.newPassword(), principal == null ? null : principal.id())));
    }
}