package com.restaurant.server.dto;

/**
 * Standard API response envelope used by every endpoint.
 * <pre>
 * success=true:  { "success": true,  "data": { ... }, "error": null, "meta": { "lang": "vi" } }
 * success=false: { "success": false, "data": null,     "error": { "code": "...", "message": "..." }, "meta": ... }
 * </pre>
 */
public record ApiResponse<T>(boolean success, T data, ApiError error, ApiMeta meta) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, ApiMeta.defaults());
    }

    public static <T> ApiResponse<T> ok(T data, ApiMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, ApiMeta.defaults());
    }

    public static <T> ApiResponse<T> error(ApiError error, ApiMeta meta) {
        return new ApiResponse<>(false, null, error, meta);
    }

    public record ApiError(String code, String message, Object fields) {
        public static ApiError of(String code, String message) {
            return new ApiError(code, message, null);
        }
    }

    public record ApiMeta(String lang, java.util.List<String> fallback) {
        public static ApiMeta defaults() {
            return new ApiMeta("vi", java.util.List.of());
        }
    }
}