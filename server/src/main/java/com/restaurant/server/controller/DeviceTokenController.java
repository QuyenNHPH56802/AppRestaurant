package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.MeDtos;
import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.DeviceTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V2.3 — Device-token endpoints for the authenticated employee.
 *
 * Endpoints:
 *   POST   /api/me/device-tokens          Idempotent upsert
 *   DELETE /api/me/device-tokens          Unregister one token
 *   GET    /api/me/device-tokens/count    Active count for the caller
 *
 * All endpoints rely on the JWT filter's {@link JwtAuthFilter.AuthenticatedUser}
 * to identify the caller; we never trust a user_id from the body.
 */
@RestController
@RequestMapping("/api/me/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService service;

    public DeviceTokenController(DeviceTokenService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeDtos.DeviceTokenStatusResponse>> register(
            @Valid @RequestBody MeDtos.DeviceTokenRegisterRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        DeviceToken.Platform p = service.register(
                userId, req.token(), req.platform(), req.deviceId(), req.appVersion());
        long active = service.countActive(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.DeviceTokenStatusResponse(true, true, (int) active, p.name())));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<MeDtos.DeviceTokenStatusResponse>> unregister(
            @Valid @RequestBody MeDtos.DeviceTokenUnregisterRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        service.unregister(userId, req.token());
        long active = service.countActive(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.DeviceTokenStatusResponse(false, false, (int) active, null)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<MeDtos.UnreadCountResponse>> count(
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.UnreadCountResponse(service.countActive(userId))));
    }

    private static Long requireUserId(JwtAuthFilter.AuthenticatedUser principal) {
        if (principal == null) {
            // JwtAuthFilter normally sets this; defensive guard.
            throw com.restaurant.server.exception.AppException
                    .unauthorized("Authentication required");
        }
        return principal.id();
    }
}
