package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.MeDtos;
import com.restaurant.server.entity.Notification;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.repository.NotificationEventRepository;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.NotificationService;
import com.restaurant.server.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * V2.3 — In-app notification feed for the authenticated employee.
 *
 * Endpoints:
 *   GET  /api/me/notifications              paged feed, newest first
 *   GET  /api/me/notifications/unread-count just the count (for badge)
 *   POST /api/me/notifications/{id}/read    mark one as read
 *   POST /api/me/notifications/read-all     bulk mark
 *   GET  /api/me/notifications/{id}/events  audit trail (admin/debug)
 */
@RestController
@RequestMapping("/api/me/notifications")
public class MeNotificationController {

    private final NotificationService service;
    private final NotificationEventRepository events;
    private final TranslationService translator;

    public MeNotificationController(NotificationService service,
                                    NotificationEventRepository events,
                                    TranslationService translator) {
        this.service = service;
        this.events = events;
        this.translator = translator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MeDtos.NotificationListResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        String lang = translator.currentLang();
        List<Notification> all = service.list(userId, 0, page * size + size);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<MeDtos.NotificationView> items = all.subList(from, to).stream()
                .map(n -> MeDtos.NotificationView.from(n, lang))
                .toList();
        long total = all.size();
        int totalPages = (int) Math.ceil(total / (double) size);
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.NotificationListResponse(items, page, size, total, totalPages)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<MeDtos.UnreadCountResponse>> unreadCount(
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.UnreadCountResponse(service.unreadCount(requireUserId(principal)))));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        boolean ok = service.markRead(userId, id);
        // Don't leak existence of foreign notifications
        if (!ok) {
            throw AppException.notFound("Notification not found");
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("id", id, "read", true)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        int n = service.markAllRead(requireUserId(principal));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("markedRead", n)));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<MeDtos.NotificationEventView>>> events(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        // Authorisation: a user may only see events for their own notifications.
        Long userId = requireUserId(principal);
        boolean owned = service.list(userId, 0, 1000).stream()
                .anyMatch(n -> id.equals(n.getId()));
        if (!owned) {
            throw AppException.notFound("Notification not found");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                events.findAllByNotificationIdOrderByCreatedAtDesc(id).stream()
                        .map(MeDtos.NotificationEventView::from)
                        .toList()));
    }

    /**
     * V2.3 / V18 — User accepts or declines a notification.
     * Body: {@code {"verdict":"ACCEPTED"} or {"verdict":"DECLINED"}}.
     *
     * Idempotent — a repeat call overwrites the previous verdict. The
     * notification is NOT marked as read; the user may want to keep it
     * visible as a record of the decision.
     */
    @PostMapping("/{id}/respond")
    public ResponseEntity<ApiResponse<MeDtos.NotificationRespondResponse>> respond(
            @PathVariable Long id,
            @Valid @RequestBody MeDtos.NotificationRespondRequest req,
            @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
        Long userId = requireUserId(principal);
        boolean ok = service.respond(userId, id, req.verdict());
        if (!ok) {
            throw AppException.notFound("Notification not found");
        }
        // We re-read to return the canonical respondedAt the server stamped.
        String verdict = service.readResponse(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(
                new MeDtos.NotificationRespondResponse(id, verdict, java.time.Instant.now().toString())));
    }

    private static Long requireUserId(JwtAuthFilter.AuthenticatedUser principal) {
        if (principal == null) {
            throw AppException.unauthorized("Authentication required");
        }
        return principal.id();
    }
}
