package com.restaurant.server.dto;

import java.util.List;

/**
 * DTOs for category/food/store that always carry a single-language "active" payload
 * plus the optional full translation map. The active payload is filled by the
 * TranslationService with fallback to vi.
 */
public class ContentDtos {

    public record TranslationPayload(
        String lang,
        String name,
        String description,
        String ingredients,
        String portion
    ) {}

    public record CategoryView(
        Long id,
        Integer sortOrder,
        String status,
        String imageUrl,
        String name,
        String description,
        String lang,
        List<String> fallback
    ) {}

    public record FoodView(
        Long id,
        Long categoryId,
        String categoryName,
        String price,
        String imageUrl,
        String status,
        boolean featured,
        Integer sortOrder,
        String name,
        String description,
        String ingredients,
        String portion,
        String lang,
        List<String> fallback,
        List<String> images
    ) {}

    public record StoreView(
        Long id,
        String logoUrl,
        String address,
        String phone,
        String openingHours,
        String name,
        String description,
        String lang,
        List<String> fallback
    ) {}

    public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages
    ) {}
}