package com.restaurant.server.service;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.entity.NotificationEvent;
import com.restaurant.server.entity.User;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.notify.NotificationProvider;
import com.restaurant.server.notify.NotificationRequest;
import com.restaurant.server.notify.TokenResult;
import com.restaurant.server.repository.DeviceTokenRepository;
import com.restaurant.server.repository.NotificationEventRepository;
import com.restaurant.server.repository.NotificationRepository;
import com.restaurant.server.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2.3 / Phase E — End-to-end test for the FAILED-event retry sweep.
 *
 * Wires a fresh {@link NotificationService} with a toggleable test
 * provider (no Spring autowiring) so we have full control over what
 * the dispatch returns. The {@link NotificationRetryService} under
 * test is the real Spring bean (it only depends on repositories).
 */
@SpringBootTest
@Transactional
class NotificationRetryServiceTest {

    @Autowired NotificationRepository notifications;
    @Autowired NotificationEventRepository events;
    @Autowired DeviceTokenRepository tokens;
    @Autowired DeviceTokenService deviceTokens;
    @Autowired UserRepository users;
    @Autowired MessageService messages;
    @Autowired RestaurantProperties props;

    @PersistenceContext EntityManager em;

    /** Manually-wired NotificationService + NotificationRetryService. */
    private NotificationService notificationService;
    private NotificationRetryService retryService;
    private ToggleProvider toggleProvider;

    private Long userId;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        events.deleteAll();
        tokens.deleteAll();
        em.flush();

        toggleProvider = new ToggleProvider();
        notificationService = new NotificationService(
                notifications, events, tokens, users,
                toggleProvider, deviceTokens, messages, props,
                new com.fasterxml.jackson.databind.ObjectMapper());
        // Wire the retry service to use our manual NotificationService so
        // its retry path actually goes through the toggle provider.
        retryService = new NotificationRetryService(events, notificationService, props);

        User u = users.findByUsername("nhanvien01").orElseThrow();
        userId = u.getId();
        deviceTokens.register(userId, "retry-tok-A", "ANDROID", null, null);
        em.flush();
        // Confirm the token is active before running tests
        long activeCount = tokens.countByUserIdAndIsActive(userId, 1);
        assertTrue(activeCount >= 1, "setUp: token must be active");
    }

    static class ToggleProvider implements NotificationProvider {
        enum Mode { RETRYABLE, SENT }
        final AtomicInteger totalCalls = new AtomicInteger(0);
        volatile Mode mode = Mode.RETRYABLE;

        @Override public String name() { return "toggle"; }
        @Override public boolean isReady() { return true; }

        @Override
        public List<TokenResult> send(NotificationRequest request) {
            totalCalls.incrementAndGet();
            List<TokenResult> out = new ArrayList<>(request.tokens().size());
            for (DeviceToken t : request.tokens()) {
                if (mode == Mode.SENT) {
                    out.add(TokenResult.sent(t.getToken(), "msg-" + totalCalls.get()));
                } else {
                    out.add(TokenResult.retryable(t.getToken(), "UNAVAILABLE", "test retryable"));
                }
            }
            return out;
        }
    }

    @Test
    void retrySweepReattemptsFailedEvent() {
        toggleProvider.mode = ToggleProvider.Mode.RETRYABLE;
        Long notifId = notificationService.createAndDispatch(userId, "SHIFT_ASSIGNED",
                Map.of("vi", "T", "ko", "T"),
                Map.of("vi", "B", "ko", "B"),
                "{\"x\":1}", "retry-test-1");
        assertNotNull(notifId);

        List<NotificationEvent> failed = events.findAllByNotificationIdOrderByCreatedAtDesc(notifId);
        assertEquals(1, failed.size());
        assertEquals(NotificationEvent.Status.FAILED, failed.get(0).getStatus());

        // Backdate the failed event so backoff is elapsed.
        NotificationEvent orig = failed.get(0);
        orig.setLastAttemptAt(Instant.now().minusSeconds(3600));
        orig.setAttempts(1);
        events.save(orig);
        em.flush();

        // Flip the provider to success.
        toggleProvider.mode = ToggleProvider.Mode.SENT;

        int attempted = retryService.retryEligible();
        assertTrue(attempted >= 1, "retry sweep should have re-attempted our event");

        List<NotificationEvent> after = events.findAllByNotificationIdOrderByCreatedAtDesc(notifId);
        assertEquals(2, after.size(), "expected 2 event rows (FAILED then SENT)");
        assertEquals(NotificationEvent.Status.SENT, after.get(0).getStatus());
        assertEquals(NotificationEvent.Status.FAILED, after.get(1).getStatus());
    }

    @Test
    void retrySweepRespectsBackoff() {
        toggleProvider.mode = ToggleProvider.Mode.RETRYABLE;
        Long notifId = notificationService.createAndDispatch(userId, "SHIFT_ASSIGNED",
                Map.of("vi", "T", "ko", "T"),
                Map.of("vi", "B", "ko", "B"),
                null, "retry-test-backoff");
        NotificationEvent orig = events.findAllByNotificationIdOrderByCreatedAtDesc(notifId).get(0);
        orig.setLastAttemptAt(Instant.now());
        orig.setAttempts(1);
        events.save(orig);
        em.flush();

        toggleProvider.mode = ToggleProvider.Mode.SENT;
        int attempted = retryService.retryEligible();
        assertEquals(0, attempted, "should NOT retry within backoff window");
        assertEquals(1, events.findAllByNotificationIdOrderByCreatedAtDesc(notifId).size());
    }

    @Test
    void retrySweepSkipsEventsAlreadyAtMaxAttempts() {
        toggleProvider.mode = ToggleProvider.Mode.RETRYABLE;
        Long notifId = notificationService.createAndDispatch(userId, "SHIFT_ASSIGNED",
                Map.of("vi", "T", "ko", "T"),
                Map.of("vi", "B", "ko", "B"),
                null, "retry-test-maxed");
        NotificationEvent orig = events.findAllByNotificationIdOrderByCreatedAtDesc(notifId).get(0);
        orig.setAttempts(props.getFcm().getRetryMaxAttempts());
        orig.setLastAttemptAt(Instant.now().minusSeconds(3600));
        events.save(orig);
        em.flush();

        toggleProvider.mode = ToggleProvider.Mode.SENT;
        int attempted = retryService.retryEligible();
        assertEquals(0, attempted, "must not retry beyond maxAttempts");
    }

    @Test
    void backoffScheduleMatchesDoc() {
        assertEquals(1, NotificationRetryService.backoffMinutesFor(0));
        assertEquals(5, NotificationRetryService.backoffMinutesFor(1));
        assertEquals(30, NotificationRetryService.backoffMinutesFor(2));
        assertEquals(120, NotificationRetryService.backoffMinutesFor(3));
        assertEquals(720, NotificationRetryService.backoffMinutesFor(4));
        assertEquals(720, NotificationRetryService.backoffMinutesFor(99), "capped at last entry");
    }
}
