package com.restaurant.server.service;

import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * V2.3 — Device-token management for push notifications.
 *
 * Responsibilities:
 *   - Idempotent register / unregister for FCM (and future APNs / WebPush) tokens.
 *   - Logout-side mass-disable (so a borrowed device cannot keep receiving pushes).
 *   - Stale-token cleanup job (deactivate tokens last-seen > N days).
 *
 * Security rules:
 *   - We never log the token itself; only the user_id and platform.
 *   - We never echo the token back to the client; the {@code MeDtos} response
 *     shape is counts + flags, not the secret.
 *   - A user can only manage their own tokens. Cross-user operations go
 *     through admin endpoints (not implemented in V2.3; admin sees only a
 *     count via /api/me/admin/users/{id}/device-count, deferred).
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final DeviceTokenRepository tokens;
    private final AuditLogRepository auditLogs;

    public DeviceTokenService(DeviceTokenRepository tokens, AuditLogRepository auditLogs) {
        this.tokens = tokens;
        this.auditLogs = auditLogs;
    }

    /**
     * Idempotent upsert. If the same (user, token) is already registered, refresh
     * its last-seen / device-id / app-version fields. Otherwise create a new
     * active row.
     *
     * @return the {@link DeviceToken.Platform} of the (now-registered) token.
     */
    @Transactional
    public DeviceToken.Platform register(Long userId,
                                         String token,
                                         String platform,
                                         String deviceId,
                                         String appVersion) {
        DeviceToken.Platform p = parsePlatform(platform);
        Instant now = Instant.now();
        DeviceToken row = tokens.findFirstByUserIdAndToken(userId, token)
                .orElse(null);
        if (row == null) {
            row = new DeviceToken();
            row.setUserId(userId);
            row.setToken(token);
            row.setPlatform(p);
            row.setIsActive(1);
        }
        row.setDeviceId(deviceId);
        row.setAppVersion(appVersion);
        row.setLastSeenAt(now);
        row.setIsActive(1);
        tokens.save(row);
        audit("DEVICE_REGISTER", userId, "platform=" + p.name());
        return p;
    }

    /**
     * Deactivate a single token (used by logout from one device).
     * If the token does not belong to {@code userId}, this is a no-op —
     * we never disclose whether the token exists or belongs to someone else.
     */
    @Transactional
    public boolean unregister(Long userId, String token) {
        if (token == null || token.isBlank()) return false;
        DeviceToken row = tokens.findFirstByUserIdAndToken(userId, token).orElse(null);
        if (row == null) {
            // Defensive: do not leak existence. Return true so the client
            // believes the unregister succeeded (idempotent logout semantics).
            return true;
        }
        row.setIsActive(0);
        row.setLastSeenAt(Instant.now());
        tokens.save(row);
        audit("DEVICE_UNREGISTER", userId, "platform=" + row.getPlatform().name());
        return true;
    }

    /**
     * Deactivate every active token for a user. Called from the logout flow
     * so a shared device cannot keep receiving the next user's pushes.
     */
    @Transactional
    public int unregisterAllForUser(Long userId) {
        int n = 0;
        for (DeviceToken t : tokens.findAllByUserId(userId)) {
            if (t.getIsActive() != null && t.getIsActive() == 1) {
                t.setIsActive(0);
                t.setLastSeenAt(Instant.now());
                tokens.save(t);
                n++;
            }
        }
        if (n > 0) audit("DEVICE_UNREGISTER_ALL", userId, "count=" + n);
        return n;
    }

    /**
     * Called by the FCM layer when Firebase reports {@code UNREGISTERED}.
     * Idempotent. Returns the row id if a token was disabled, else empty.
     */
    @Transactional
    public java.util.Optional<Long> deactivateByToken(String token) {
        if (token == null || token.isBlank()) return java.util.Optional.empty();
        return tokens.findFirstByToken(token).map(row -> {
            if (row.getIsActive() != null && row.getIsActive() == 1) {
                row.setIsActive(0);
                row.setLastSeenAt(Instant.now());
                tokens.save(row);
                audit("DEVICE_FCM_UNREGISTERED", row.getUserId(), "platform=" + row.getPlatform().name());
            }
            return row.getId();
        });
    }

    /** Count of currently-active tokens for a user. Returned to the device
     *  so the user can verify "my 2 phones are registered". Never includes
     *  inactive rows. */
    @Transactional(readOnly = true)
    public long countActive(Long userId) {
        return tokens.countByUserIdAndIsActive(userId, 1);
    }

    /**
     * Cleanup job entry point. Disables tokens last-seen before the cutoff.
     * Safe to call from a scheduled task. Returns number of rows disabled.
     */
    @Transactional
    public int deactivateStaleTokens(int staleAfterDays) {
        Instant cutoff = Instant.now().minus(staleAfterDays, ChronoUnit.DAYS);
        int n = tokens.deactivateStale(cutoff, Instant.now());
        if (n > 0) log.info("deactivated {} stale device tokens (cutoff={})", n, cutoff);
        return n;
    }

    private static DeviceToken.Platform parsePlatform(String s) {
        if (s == null) {
            throw new IllegalArgumentException("platform is required");
        }
        return DeviceToken.Platform.valueOf(s.toUpperCase(Locale.ROOT));
    }

    private void audit(String action, Long userId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("device_token");
            a.setEntityId(userId == null ? null : userId.toString());
            a.setUserId(userId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception e) {
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
