package com.restaurant.server.service;

import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.entity.User;
import com.restaurant.server.repository.DeviceTokenRepository;
import com.restaurant.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for V2.3 DeviceTokenService.
 *
 * Each test starts from a clean slate of device_tokens (we don't reset users —
 * they're seeded by V2). Tests run inside a real Spring context so the JPA
 * mapping + UNIQUE constraint are actually exercised.
 */
@SpringBootTest
@Transactional
class DeviceTokenServiceTest {

    @Autowired DeviceTokenService service;
    @Autowired DeviceTokenRepository tokens;
    @Autowired UserRepository users;

    private Long userId;

    @BeforeEach
    void setUp() {
        tokens.deleteAll();
        // Use the seed STAFF user (nhanvien01)
        User u = users.findByUsername("nhanvien01").orElseThrow();
        userId = u.getId();
    }

    @Test
    void registerCreatesNewActiveRow() {
        DeviceToken.Platform p = service.register(userId, "tok-1", "ANDROID", "dev-a", "1.0.0");
        assertEquals(DeviceToken.Platform.ANDROID, p);

        DeviceToken row = tokens.findFirstByUserIdAndToken(userId, "tok-1").orElseThrow();
        assertEquals(1, row.getIsActive());
        assertEquals("dev-a", row.getDeviceId());
        assertEquals("1.0.0", row.getAppVersion());
        assertNotNull(row.getLastSeenAt());
    }

    @Test
    void registerIsIdempotentForSameToken() {
        service.register(userId, "tok-dup", "ANDROID", "dev-a", "1.0.0");
        Instant firstSeen = tokens.findFirstByUserIdAndToken(userId, "tok-dup").orElseThrow().getLastSeenAt();

        // Sleep long enough to observe a measurable change in lastSeenAt.
        try { Thread.sleep(20); } catch (InterruptedException ignored) {}

        service.register(userId, "tok-dup", "ANDROID", "dev-a", "1.0.1");
        assertEquals(1, tokens.count(), "must not create a duplicate row");

        DeviceToken row = tokens.findFirstByUserIdAndToken(userId, "tok-dup").orElseThrow();
        assertTrue(row.getLastSeenAt().isAfter(firstSeen), "last_seen_at should refresh");
        assertEquals("1.0.1", row.getAppVersion());
    }

    @Test
    void unregisterDisablesTokenForSameUser() {
        service.register(userId, "tok-out", "ANDROID", null, null);
        boolean ok = service.unregister(userId, "tok-out");
        assertTrue(ok);
        DeviceToken row = tokens.findFirstByUserIdAndToken(userId, "tok-out").orElseThrow();
        assertEquals(0, row.getIsActive());
    }

    @Test
    void unregisterIsNoopForTokenOfOtherUser() {
        // Use admin user to plant a token
        Long otherId = users.findByUsername("admin").orElseThrow().getId();
        service.register(otherId, "tok-other", "ANDROID", null, null);

        // Caller is nhanvien01; should not affect admin's token
        boolean ok = service.unregister(userId, "tok-other");
        assertTrue(ok, "unregister returns true even if caller doesn't own the token (no info leak)");
        DeviceToken row = tokens.findFirstByToken("tok-other").orElseThrow();
        assertEquals(1, row.getIsActive(), "admin's token must remain active");
        assertNotEquals(userId, row.getUserId());
    }

    @Test
    void unregisterAllForUserDisablesEveryActiveToken() {
        service.register(userId, "tok-1", "ANDROID", null, null);
        service.register(userId, "tok-2", "IOS", null, null);
        service.register(userId, "tok-3", "WEB", null, null);
        assertEquals(3, tokens.count());

        int n = service.unregisterAllForUser(userId);
        assertEquals(3, n);
        assertEquals(0, tokens.countByUserIdAndIsActive(userId, 1));
    }

    @Test
    void deactivateByTokenUsedByFcmLayer() {
        service.register(userId, "tok-fcm", "ANDROID", null, null);
        var maybeId = service.deactivateByToken("tok-fcm");
        assertTrue(maybeId.isPresent());
        DeviceToken row = tokens.findFirstByToken("tok-fcm").orElseThrow();
        assertEquals(0, row.getIsActive());
    }

    @Test
    void deactivateByTokenIsIdempotent() {
        service.register(userId, "tok-idem", "ANDROID", null, null);
        service.deactivateByToken("tok-idem");
        service.deactivateByToken("tok-idem"); // second call: no exception, no change
        assertEquals(0,
                tokens.findFirstByToken("tok-idem").orElseThrow().getIsActive());
    }

    @Test
    void countActiveReturnsOnlyActiveRows() {
        service.register(userId, "a", "ANDROID", null, null);
        service.register(userId, "b", "IOS", null, null);
        service.register(userId, "c", "WEB", null, null);
        assertEquals(3, service.countActive(userId));

        service.unregister(userId, "b");
        assertEquals(2, service.countActive(userId));
    }

    @Test
    void staleTokensGetDeactivatedByCleanupJob() {
        DeviceToken old = new DeviceToken();
        old.setUserId(userId);
        old.setToken("tok-stale");
        old.setPlatform(DeviceToken.Platform.ANDROID);
        // Well past the 180-day cutoff
        old.setLastSeenAt(Instant.now().minus(365, ChronoUnit.DAYS));
        old.setIsActive(1);
        tokens.saveAndFlush(old);

        DeviceToken fresh = new DeviceToken();
        fresh.setUserId(userId);
        fresh.setToken("tok-fresh");
        fresh.setPlatform(DeviceToken.Platform.ANDROID);
        fresh.setLastSeenAt(Instant.now());
        fresh.setIsActive(1);
        tokens.saveAndFlush(fresh);

        int n = service.deactivateStaleTokens(180);
        assertTrue(n >= 1, "expected at least 1 stale token deactivated, got " + n);

        assertEquals(0, tokens.findFirstByToken("tok-stale").orElseThrow().getIsActive());
        assertEquals(1, tokens.findFirstByToken("tok-fresh").orElseThrow().getIsActive());
    }

    @Test
    void differentUsersWithSameTokenAreIndependent() {
        Long adminId = users.findByUsername("admin").orElseThrow().getId();
        service.register(userId, "shared", "ANDROID", null, null);
        service.register(adminId, "shared", "ANDROID", null, null);
        // Both rows exist with the same token string — UNIQUE(user_id, token)
        // allows the pair (userA, token) and (userB, token) to coexist.
        long total = tokens.count();
        assertEquals(2, total, "same FCM token from two users must produce two distinct rows");
        // Sanity: each user has their own active row
        assertEquals(1, service.countActive(userId));
        assertEquals(1, service.countActive(adminId));
    }

    @Test
    void registerRejectsUnknownPlatform() {
        boolean threw = false;
        try {
            service.register(userId, "tok", "BLACKBERRY", null, null);
        } catch (IllegalArgumentException ex) {
            threw = true;
        }
        assertTrue(threw, "service must refuse unknown platform values");
        assertFalse(tokens.findFirstByUserIdAndToken(userId, "tok").isPresent());
    }
}
