package com.restaurant.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 200) String password
    ) {}

    public record LoginResponse(
        String token,
        long expiresInSeconds,
        UserSummary user
    ) {}

    public record UserSummary(
        Long id,
        String username,
        String fullName,
        String role,
        String lang
    ) {}

    public record LogoutResponse(String message) {}
}