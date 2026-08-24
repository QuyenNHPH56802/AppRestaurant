package com.restaurant.server.service;

import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.entity.Notification;
import com.restaurant.server.entity.NotificationEvent;
import com.restaurant.server.entity.User;
import com.restaurant.server.repository.DeviceTokenRepository;
import com.restaurant.server.repository.NotificationEventRepository;
import com.restaurant.server.repository.NotificationRepository;
import com.restaurant.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2.3 — Integration tests for NotificationService.
 *
 * Exercises createAndDispatch end-to-end through the noop provider so we
 * don't need real Firebase credentials. The audit-trail behaviour is the
 * actual thing under test, not FCM transport.
 */
@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired NotificationService service;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationEventRepository events;
    @Autowired DeviceTokenRepository tokens;
    @Autowired UserRepository users;
    @Autowired DeviceTokenService deviceTokens;

    private Long userId;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        events.deleteAll();
        tokens.deleteAll();
        User u = users.findByUsername("nhanvien01").orElseThrow();
        userId = u.getId();
    }

    @Test
    void createAndDispatchPersistsNotificationAndEvent() {
        Long id = service.createAndDispatch(
                userId, "SHIFT_ASSIGNED",
                Map.of("vi", "Phân ca mới", "ko", "새 근무 배정"),
                Map.of("vi", "Bạn có ca mới", "ko", "새 근무가 있습니다"),
                "{\"shiftId\":42}",
                "idem-1");
        assertNotNull(id);

        Notification n = notifications.findById(id).orElseThrow();
        assertEquals("SHIFT_ASSIGNED", n.getType());
        assertEquals("Phân ca mới", n.getTitleVi());
        assertEquals("새 근무 배정", n.getTitleKo());
        assertEquals("idem-1", n.getIdempotencyKey());

        // No active device tokens -> SKIPPED row with NO_TOKEN
        List<NotificationEvent> evs = events.findAllByNotificationIdOrderByCreatedAtDesc(id);
        assertEquals(1, evs.size());
        assertEquals(NotificationEvent.Status.SKIPPED, evs.get(0).getStatus());
        assertEquals("NO_TOKEN", evs.get(0).getErrorCode());
    }

    @Test
    void createAndDispatchSendsToActiveTokens() {
        // Register two active tokens for this user
        deviceTokens.register(userId, "tok-A", "ANDROID", "dev-a", "1.0");
        deviceTokens.register(userId, "tok-B", "IOS", "dev-b", "1.0");

        Long id = service.createAndDispatch(
                userId, "ZONE_CHANGED",
                Map.of("vi", "T", "ko", "T"),
                Map.of("vi", "B", "ko", "B"),
                null, "idem-2");

        List<NotificationEvent> evs = events.findAllByNotificationIdOrderByCreatedAtDesc(id);
        assertEquals(2, evs.size(), "one event row per active token");
        // All tokens got SKIPPED because we run with noop provider
        for (NotificationEvent ev : evs) {
            assertEquals(NotificationEvent.Status.SKIPPED, ev.getStatus());
            assertNotNull(ev.getProvider());
        }
    }

    @Test
    void createAndDispatchIsIdempotentByKey() {
        Long id1 = service.createAndDispatch(userId, "SHIFT_ASSIGNED",
                Map.of("vi", "A", "ko", "A"),
                Map.of("vi", "B", "ko", "B"),
                null, "shared-key");
        Long id2 = service.createAndDispatch(userId, "SHIFT_ASSIGNED",
                Map.of("vi", "DIFFERENT", "ko", "DIFFERENT"),
                Map.of("vi", "DIFFERENT", "ko", "DIFFERENT"),
                null, "shared-key");

        assertEquals(id1, id2, "idempotencyKey must reuse the original notification");
        assertEquals(1L, notifications.count(), "must not create a duplicate row");

        // Dispatch was retried -> a 2nd SKIPPED row exists
        List<NotificationEvent> evs = events.findAllByNotificationIdOrderByCreatedAtDesc(id1);
        assertEquals(2, evs.size());
    }

    @Test
    void createAndDispatchWithoutIdempotencyKeyAlwaysCreatesNew() {
        Long id1 = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"),
                Map.of("vi", "x", "ko", "x"),
                null, null);
        Long id2 = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "y", "ko", "y"),
                Map.of("vi", "y", "ko", "y"),
                null, null);

        assertTrue(id1 != null && id2 != null && !id1.equals(id2),
                "without idempotency key each call is distinct");
        assertEquals(2L, notifications.count());
    }

    @Test
    void markReadOnlyTogglesOwnNotification() {
        Long adminId = users.findByUsername("admin").orElseThrow().getId();
        Long mineId = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        Long theirsId = service.createAndDispatch(adminId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);

        assertTrue(service.markRead(userId, mineId));
        Notification mine = notifications.findById(mineId).orElseThrow();
        assertNotNull(mine.getReadAt());

        // Caller can't mark someone else's notification — must return false
        // (controller converts that to 404).
        assertEquals(false, service.markRead(userId, theirsId));
    }

    @Test
    void unreadCountReflectsState() {
        assertEquals(0L, service.unreadCount(userId));

        Long id1 = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        Long id2 = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        assertEquals(2L, service.unreadCount(userId));

        service.markRead(userId, id1);
        assertEquals(1L, service.unreadCount(userId));
        service.markRead(userId, id2);
        assertEquals(0L, service.unreadCount(userId));
    }

    @Test
    void markAllReadBulkClearsUnread() {
        service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        int n = service.markAllRead(userId);
        assertEquals(2, n);
        assertEquals(0L, service.unreadCount(userId));
    }

    @Test
    void listReturnsNewestFirst() {
        Long first = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "a", "ko", "a"), Map.of("vi", "a", "ko", "a"),
                null, null);
        Long second = service.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "b", "ko", "b"), Map.of("vi", "b", "ko", "b"),
                null, null);
        List<Notification> page = service.list(userId, 0, 10);
        assertEquals(2, page.size());
        assertEquals(second, page.get(0).getId(), "newest first");
        assertEquals(first, page.get(1).getId());
    }

    @Test
    void dispatchDeactivatesTokensWhenPermanentFailure() {
        // Use a custom in-test provider that returns PERMANENT_FAILURE
        // to prove NotificationService wires the deactivateByToken side-effect.
        com.restaurant.server.notify.NotificationProvider permanent =
                new com.restaurant.server.notify.NotificationProvider() {
                    @Override public String name() { return "test-perm"; }
                    @Override public boolean isReady() { return true; }
                    @Override public List<com.restaurant.server.notify.TokenResult> send(
                            com.restaurant.server.notify.NotificationRequest req) {
                        return req.tokens().stream()
                                .map(t -> com.restaurant.server.notify.TokenResult.permanent(
                                        t.getToken(), "UNREGISTERED", "test"))
                                .toList();
                    }
                };
        // Re-wire service with this provider for this test only
        NotificationService override = new NotificationService(
                notifications, events, tokens, users, permanent, deviceTokens,
                org.mockito.Mockito.mock(com.restaurant.server.i18n.MessageService.class),
                org.mockito.Mockito.mock(com.restaurant.server.config.RestaurantProperties.class),
                new com.fasterxml.jackson.databind.ObjectMapper());

        deviceTokens.register(userId, "to-be-killed", "ANDROID", null, null);
        assertEquals(1, tokens.countByUserIdAndIsActive(userId, 1));

        override.createAndDispatch(userId, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);

        assertEquals(0, tokens.countByUserIdAndIsActive(userId, 1),
                "PERMANENT_FAILURE should have deactivated the token");
    }

    @Test
    void missingUserReturnsNullAndDoesNotThrow() {
        Long id = service.createAndDispatch(99999L, "MANAGER_MESSAGE",
                Map.of("vi", "x", "ko", "x"), Map.of("vi", "y", "ko", "y"),
                null, null);
        assertEquals(null, id, "missing user -> null, no exception");
    }
}
