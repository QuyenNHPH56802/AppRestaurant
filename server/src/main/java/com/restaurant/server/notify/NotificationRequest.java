package com.restaurant.server.notify;

import com.restaurant.server.entity.DeviceToken;

import java.util.List;
import java.util.Map;

/**
 * V2.3 — Payload passed from {@link com.restaurant.server.service.NotificationService}
 * to a {@link NotificationProvider}.
 *
 * The provider gets:
 *   - a notification type (drives icon / channel category on the device)
 *   - locale-tagged title/body pairs (Android picks the right one per user)
 *   - a free-form data map (always strings, never binary — FCM data payload
 *     constraint). The server's internal notification id is ALWAYS included
 *     under the {@code "nid"} key so a tap on the phone can deep-link back
 *     via {@code /api/me/notifications/{id}}.
 *   - the list of device tokens to target (all belong to the same user)
 */
public record NotificationRequest(
        String type,
        Map<String, String> titleByLang, // "vi" -> "...", "ko" -> "..."
        Map<String, String> bodyByLang,
        Map<String, String> data,
        List<DeviceToken> tokens
) {
    public NotificationRequest {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("at least one device token is required");
        }
        if (titleByLang == null) titleByLang = Map.of();
        if (bodyByLang == null) bodyByLang = Map.of();
        if (data == null) data = Map.of();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String type;
        private Map<String, String> titleByLang = Map.of();
        private Map<String, String> bodyByLang = Map.of();
        private Map<String, String> data = Map.of();
        private List<DeviceToken> tokens = List.of();

        public Builder type(String v) { this.type = v; return this; }
        public Builder titleByLang(Map<String, String> v) { this.titleByLang = v; return this; }
        public Builder bodyByLang(Map<String, String> v) { this.bodyByLang = v; return this; }
        public Builder data(Map<String, String> v) { this.data = v; return this; }
        public Builder tokens(List<DeviceToken> v) { this.tokens = v; return this; }
        public Builder putData(String k, String v) {
            this.data = new java.util.HashMap<>(this.data);
            this.data.put(k, v);
            return this;
        }

        public NotificationRequest build() {
            return new NotificationRequest(type, titleByLang, bodyByLang, data, tokens);
        }
    }
}
