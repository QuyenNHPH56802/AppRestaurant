package com.restaurant.server.dto;

import com.restaurant.server.entity.Notification;
import com.restaurant.server.entity.NotificationEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * V2.3 — DTOs for the {@code /api/me/*} namespace (device tokens, notifications).
 *
 * The token NEVER appears in any view shape: the controller only returns
 * counts + status flags. Even for the authenticated owner, we never echo the
 * token back over the wire — once registered, the device has no reason to
 * ask for it again.
 */
public class MeDtos {

    // ----------------- Device tokens -----------------

    /**
     * Body of {@code POST /api/me/device-tokens}.
     * {@code platform} is locked to the documented enum values so a hostile
     * client can't smuggle arbitrary strings into the DB.
     */
    public record DeviceTokenRegisterRequest(
            @NotBlank @Size(max = 4096) String token,
            @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB") String platform,
            @Size(max = 256) String deviceId,
            @Size(max = 64) String appVersion
    ) {}

    /** Body of {@code DELETE /api/me/device-tokens}. */
    public record DeviceTokenUnregisterRequest(
            @NotBlank @Size(max = 4096) String token
    ) {}

    /**
     * View returned after a successful upsert / unregister. Deliberately does
     * NOT include the token — the client knows its own token.
     */
    public record DeviceTokenStatusResponse(
            boolean registered,
            boolean active,
            int activeDeviceCount,
            String platform
    ) {}

    // ----------------- Notifications -----------------

    public record NotificationView(
            Long id,
            String type,
            String title,
            String body,
            String payloadJson,
            Instant readAt,
            Instant createdAt
    ) {
        public static NotificationView from(Notification n, String lang) {
            String title = "ko".equals(lang) ? n.getTitleKo() : n.getTitleVi();
            String body  = "ko".equals(lang) ? n.getBodyKo()  : n.getBodyVi();
            return new NotificationView(
                    n.getId(), n.getType(), title, body,
                    n.getPayloadJson(), n.getReadAt(), n.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long count) {}

    public record NotificationEventView(
            Long id,
            Long notificationId,
            String channel,
            String status,
            String provider,
            String providerMsgId,
            String errorCode,
            Integer attempts,
            Instant lastAttemptAt,
            Instant createdAt
    ) {
        public static NotificationEventView from(NotificationEvent e) {
            return new NotificationEventView(
                    e.getId(), e.getNotificationId(),
                    e.getChannel().name(), e.getStatus().name(),
                    e.getProvider(), e.getProviderMsgId(),
                    e.getErrorCode(), e.getAttempts(),
                    e.getLastAttemptAt(), e.getCreatedAt());
        }
    }

    /** Generic wrapper for the /api/me/notifications list endpoint. */
    public record NotificationListResponse(
            List<NotificationView> items,
            int page,
            int size,
            long total,
            int totalPages
    ) {}

    /**
     * Body of {@code POST /api/me/notifications/{id}/respond}.
     * Verdict is locked to {@code ACCEPTED} or {@code DECLINED} so a hostile
     * client can't store arbitrary strings inside the payloadJson column.
     */
    public record NotificationRespondRequest(
            @NotBlank @Pattern(regexp = "ACCEPTED|DECLINED") String verdict
    ) {}

    /** Response body for the respond endpoint. */
    public record NotificationRespondResponse(
            Long id,
            String verdict,
            String respondedAt
    ) {}
}
