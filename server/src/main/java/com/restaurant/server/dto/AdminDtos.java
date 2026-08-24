package com.restaurant.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    // ----------------- Categories -----------------

    public record CategoryRequest(
        @NotNull Integer sortOrder,
        @NotBlank String status,
        String imageUrl,
        @NotNull @Size(min = 1) @Valid List<TranslationInput> translations
    ) {}

    public record CategoryPatchRequest(
        Integer sortOrder,
        String status,
        String imageUrl
    ) {}

    // ----------------- Foods -----------------

    public record FoodRequest(
        @NotNull Long categoryId,
        @NotNull @DecimalMin("0") BigDecimal price,
        String imageUrl,
        @NotBlank String status,
        @NotNull Boolean featured,
        @NotNull Integer sortOrder,
        @NotNull @Size(min = 1) @Valid List<FoodTranslationInput> translations
    ) {}

    public record FoodPatchRequest(
        Long categoryId,
        BigDecimal price,
        String imageUrl,
        String status,
        Boolean featured,
        Integer sortOrder
    ) {}

    // ----------------- Users -----------------

    public record UserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 200) String password,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank String role,
        @NotBlank String lang,
        @NotBlank String status
    ) {}

    public record UserPatchRequest(
        @Size(max = 200) String fullName,
        String role,
        String lang,
        String status
    ) {}

    public record UserResetPasswordRequest(
        @NotBlank @Size(max = 200) String newPassword
    ) {}

    public record UserView(
        Long id, String username, String fullName, String role, String status, String lang
    ) {}

    // ----------------- Store -----------------

    public record StoreRequest(
        String logoUrl, String address, String phone, String openingHours,
        @NotNull @Size(min = 1) @Valid List<StoreTranslationInput> translations
    ) {}

    // ----------------- Translations -----------------

    public record TranslationInput(
        @NotBlank String lang,
        @NotBlank String name,
        String description
    ) {}

    public record FoodTranslationInput(
        @NotBlank String lang,
        @NotBlank String name,
        String description,
        String ingredients,
        String portion
    ) {}

    public record StoreTranslationInput(
        @NotBlank String lang,
        @NotBlank String storeName,
        String description
    ) {}

    public record UploadResponse(String imageUrl, long size, String contentType) {}

    public record ApiErrorBody(String code, String message, Map<String, String> fields) {}
}