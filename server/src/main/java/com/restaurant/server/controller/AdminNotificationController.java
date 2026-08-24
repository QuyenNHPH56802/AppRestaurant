package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.MeDtos;
import com.restaurant.server.entity.Notification;
import com.restaurant.server.entity.User;
import com.restaurant.server.repository.DeviceTokenRepository;
import com.restaurant.server.repository.NotificationEventRepository;
import com.restaurant.server.repository.NotificationRepository;
import com.restaurant.server.repository.UserRepository;
import com.restaurant.server.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase G — Admin notification + device-token browsing endpoints.
 *
 * Backs the "Thông báo" and "Thiết bị" views in the admin SPA.
 *
 * <p>Endpoints:</p>
 * <ul>
 *     <li>{@code GET /api/admin/notifications}              — global feed, paginated.</li>
 *     <li>{@code GET /api/admin/notifications/{id}/events} — full audit trail for one notification.</li>
 *     <li>{@code GET /api/admin/device-tokens}             — all registered device tokens (sanitised).</li>
 *     <li>{@code GET /api/admin/device-tokens/stats}       — per-user active count.</li>
 * </ul>
 *
 * <p>The admin sees usernames (from {@link UserRepository#findById(Object)}) attached
 * to each notification. The token itself is NEVER returned in any payload —
 * only the platform, last-seen, and device-id (which is itself opaque).</p>
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationRepository notifications;
    private final NotificationEventRepository events;
    private final NotificationService notificationService;
    private final DeviceTokenRepository tokens;
    private final UserRepository users;

    public AdminNotificationController(NotificationRepository notifications,
                                      NotificationEventRepository events,
                                      NotificationService notificationService,
                                      DeviceTokenRepository tokens,
                                      UserRepository users) {
        this.notifications = notifications;
        this.events = events;
        this.notificationService = notificationService;
        this.tokens = tokens;
        this.users = users;
    }

    /**
     * Global notification feed. Page is 0-indexed, size capped at 200 to
     * keep the response bounded. Each row carries the recipient username
     * so the admin can scan who's getting what.
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId) {
        int safeSize = Math.max(1, Math.min(size, 200));
        List<Notification> all;
        if (userId != null) {
            all = notifications.findAllByUserIdOrderByCreatedAtDesc(userId);
        } else {
            all = notifications.findAllByOrderByCreatedAtDesc(PageRequest.of(page, safeSize));
        }
        if (type != null && !type.isBlank()) {
            all = all.stream().filter(n -> type.equals(n.getType())).toList();
        }
        List<Map<String, Object>> items = all.stream()
                .map(this::toAdminView)
                .toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", page);
        resp.put("size", safeSize);
        resp.put("total", notifications.count());
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    /** Full audit trail (one row per delivery attempt) for a single notification. */
    @GetMapping("/notifications/{id}/events")
    public ResponseEntity<ApiResponse<List<MeDtos.NotificationEventView>>> eventsFor(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                events.findAllByNotificationIdOrderByCreatedAtDesc(id).stream()
                        .map(MeDtos.NotificationEventView::from)
                        .toList()));
    }

    /**
     * List every registered device token across the install. Sanitised:
     * no raw token, no PII. Sorted newest-first by last_seen_at.
     */
    @GetMapping("/device-tokens")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> deviceTokens(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean active) {
        List<com.restaurant.server.entity.DeviceToken> all;
        if (userId != null) {
            all = tokens.findAllByUserId(userId);
        } else {
            all = tokens.findAll();
        }
        if (active != null) {
            int want = active ? 1 : 0;
            all = all.stream().filter(t -> t.getIsActive() != null && t.getIsActive() == want).toList();
        }
        // Sort newest-first by last_seen_at desc
        all = all.stream()
                .sorted((a, b) -> {
                    if (a.getLastSeenAt() == null && b.getLastSeenAt() == null) return 0;
                    if (a.getLastSeenAt() == null) return 1;
                    if (b.getLastSeenAt() == null) return -1;
                    return b.getLastSeenAt().compareTo(a.getLastSeenAt());
                })
                .toList();
        List<Map<String, Object>> rows = all.stream().map(t -> {
            Map<String, Object> r = new HashMap<>();
            r.put("id", t.getId());
            r.put("userId", t.getUserId());
            r.put("platform", t.getPlatform() == null ? null : t.getPlatform().name());
            r.put("deviceId", t.getDeviceId());
            r.put("appVersion", t.getAppVersion());
            r.put("lastSeenAt", t.getLastSeenAt() == null ? null : t.getLastSeenAt().toString());
            r.put("isActive", t.getIsActive() != null && t.getIsActive() == 1);
            r.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
            // Token preview: first 6 chars only, for diagnostic identification.
            // NEVER the full token.
            String fullToken = t.getToken() == null ? "" : t.getToken();
            r.put("tokenPreview", fullToken.length() >= 6 ? fullToken.substring(0, 6) + "..." : "***");
            r.put("tokenLength", fullToken.length());
            return r;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(rows));
    }

    /** Per-user active device-token counts. */
    @GetMapping("/device-tokens/stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> deviceTokenStats() {
        List<User> userList = users.findAll();
        List<Map<String, Object>> rows = userList.stream().map(u -> {
            Map<String, Object> r = new HashMap<>();
            r.put("userId", u.getId());
            r.put("username", u.getUsername());
            r.put("displayName", u.getFullName());
            r.put("role", u.getRole() == null ? null : u.getRole().name());
            r.put("activeCount", tokens.countByUserIdAndIsActive(u.getId(), 1));
            return r;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(rows));
    }

    private Map<String, Object> toAdminView(Notification n) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", n.getId());
        r.put("userId", n.getUserId());
        // Resolve username for the admin view (no extra round-trip from the SPA).
        users.findById(n.getUserId())
                .ifPresent(u -> r.put("username", u.getUsername()));
        r.put("type", n.getType());
        r.put("titleVi", n.getTitleVi());
        r.put("titleKo", n.getTitleKo());
        r.put("bodyVi", n.getBodyVi());
        r.put("bodyKo", n.getBodyKo());
        r.put("payloadJson", n.getPayloadJson());
        r.put("idempotencyKey", n.getIdempotencyKey());
        r.put("readAt", n.getReadAt() == null ? null : n.getReadAt().toString());
        r.put("createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
        return r;
    }
}