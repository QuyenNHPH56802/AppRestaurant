package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.AuthDtos;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final MessageService messages;

    public AuthController(AuthService auth, MessageService messages) {
        this.auth = auth;
        this.messages = messages;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest body,
            HttpServletRequest request) {
        String ip = clientIp(request);
        return ResponseEntity.ok(ApiResponse.ok(auth.login(body.username(), body.password(), ip)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthDtos.LogoutResponse>> logout(
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        if (principal == null) throw AppException.unauthorized(messages.get("error.unauthorized"));
        return ResponseEntity.ok(ApiResponse.ok(auth.logout(principal.id(), principal.username())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.UserSummary>> me(
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        if (principal == null) throw AppException.unauthorized(messages.get("error.unauthorized"));
        return ResponseEntity.ok(ApiResponse.ok(auth.me(principal.id())));
    }

    private String clientIp(HttpServletRequest request) {
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}