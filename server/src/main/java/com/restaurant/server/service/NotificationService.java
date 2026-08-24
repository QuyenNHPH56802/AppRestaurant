package com.restaurant.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.DeviceToken;
import com.restaurant.server.entity.Notification;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * V2.3 — Coordinates in-app feed + push delivery.
 *
 * Three distinct responsibilities:
 *   1. {@link #createAndDispatch} — single point of entry. Idempotent by
 *      {@code idempotencyKey}: the same key returns the original notification
 *      (and re-attempts the PUSH channel) without creating a duplicate row.
 *   2. {@link #markRead} / {@link #unreadCount} / {@link #list} — read-side.
 *   3. Token result handling — maps provider outcomes to either
 *      {@link NotificationEventRepository} rows or token deactivation.
 *
 * The dispatch is intentionally synchronous in the request path: it keeps
 * semantics simple and ensures the audit row exists before the API responds.
 * A future migration to async would only need to flip the {@code @Async}
 * annotation + introduce a queue.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final NotificationEventRepository events;
    private final DeviceTokenRepository tokens;
    private final UserRepository users;
    private final NotificationProvider provider;
    private final DeviceTokenService deviceTokens;
    private final MessageService messages;
    private final RestaurantProperties props;
    /** Used to merge {@code response} state into the existing {@code payloadJson}. */
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notifications,
                               NotificationEventRepository events,
                               DeviceTokenRepository tokens,
                               UserRepository users,
                               NotificationProvider provider,
                               DeviceTokenService deviceTokens,
                               MessageService messages,
                               RestaurantProperties props,
                               ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.events = events;
        this.tokens = tokens;
        this.users = users;
        this.provider = provider;
        this.deviceTokens = deviceTokens;
        this.messages = messages;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    // ----------------- public API -----------------

    /**
     * Idempotently create a notification + dispatch push to the user's active
     * device tokens.
     *
     * @param userId          the recipient
     * @param type            e.g. {@code SHIFT_ASSIGNED}, {@code ZONE_CHANGED}
     * @param titleByLang     "vi"/"ko" -> localised title. Falls back to
     *                        {@code messages.get("push." + type + ".title")}
     *                        when a language is missing.
     * @param bodyByLang      same fallback rule via {@code push.<type>.body}
     * @param payloadJson     optional opaque JSON to store in the notification
     *                        row so the Android app can deep-link on tap.
     * @param idempotencyKey  optional; when present, repeat calls with the
     *                        same key return the original notification id
     *                        and DO NOT create a duplicate. Backs the V18
     *                        "at-most-once" guarantee.
     * @return the notification id (existing if idempotencyKey matched)
     */
    @Transactional
    public Long createAndDispatch(Long userId,
                                  String type,
                                  Map<String, String> titleByLang,
                                  Map<String, String> bodyByLang,
                                  String payloadJson,
                                  String idempotencyKey) {
        if (userId == null || type == null || type.isBlank()) {
            throw new IllegalArgumentException("userId and type are required");
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Notification> existing = notifications.findFirstByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                // Re-attempt the PUSH dispatch only (idempotent at the row
                // level, not the transport level). Audit row will get a
                // second PENDING -> SENT line.
                Long notificationId = existing.get().getId();
                dispatch(userId, notificationId, type,
                        mergeTitles(type, titleByLang),
                        mergeBodies(type, bodyByLang),
                        payloadJson);
                return notificationId;
            }
        }
        User u = users.findById(userId).orElse(null);
        if (u == null) {
            log.warn("createAndDispatch: user {} not found, dropping", userId);
            return null;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitleVi(titleByLang == null ? null : titleByLang.get("vi"));
        n.setTitleKo(titleByLang == null ? null : titleByLang.get("ko"));
        n.setBodyVi(bodyByLang == null ? null : bodyByLang.get("vi"));
        n.setBodyKo(bodyByLang == null ? null : bodyByLang.get("ko"));
        n.setPayloadJson(payloadJson);
        n.setIdempotencyKey(idempotencyKey);
        Notification saved = notifications.save(n);
        dispatch(userId, saved.getId(), type,
                mergeTitles(type, titleByLang),
                mergeBodies(type, bodyByLang),
                payloadJson);
        return saved.getId();
    }

    /** Read-side: list the user's notifications, newest first, paginated. */
    @Transactional(readOnly = true)
    public List<Notification> list(Long userId, int page, int size) {
        return notifications.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public boolean markRead(Long userId, Long notificationId) {
        return notifications.findById(notificationId).map(n -> {
            if (!userId.equals(n.getUserId())) {
                // Don't disclose that the notification exists for someone else.
                return false;
            }
            if (n.getReadAt() == null) {
                n.setReadAt(Instant.now());
                notifications.save(n);
            }
            return true;
        }).orElse(false);
    }

    @Transactional
    public int markAllRead(Long userId) {
        int n = 0;
        for (Notification notif : notifications.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)) {
            notif.setReadAt(Instant.now());
            notifications.save(notif);
            n++;
        }
        return n;
    }

    /**
     * V2.3 / V18 — User accepts or declines a notification (typically a
     * SHIFT_ASSIGNED push). The response is encoded inside the existing
     * {@code payloadJson} under the {@code response} key so we don't have
     * to ship a new column + migration.
     *
     * Idempotent: a second call with the same verdict simply overwrites the
     * previous response. We do NOT mark the notification as read here —
     * the user may want to keep it visible in the feed as a record of the
     * decision. They can swipe it from the in-app feed or mark-all-read.
     *
     * Returns true on success; false when the notification doesn't exist or
     * belongs to another user (controller translates false → 404 to avoid
     * disclosing existence).
     */
    @Transactional
    public boolean respond(Long userId, Long notificationId, String verdict) {
        if (verdict == null) return false;
        String normalised = verdict.trim().toUpperCase(Locale.ROOT);
        if (!"ACCEPTED".equals(normalised) && !"DECLINED".equals(normalised)) {
            return false;
        }
        return notifications.findById(notificationId).map(n -> {
            if (!userId.equals(n.getUserId())) return false;
            n.setPayloadJson(mergeResponse(n.getPayloadJson(), normalised, Instant.now()));
            notifications.save(n);
            return true;
        }).orElse(false);
    }

    /** Read the user's verdict for a notification. Null if not yet responded. */
    @Transactional(readOnly = true)
    public String readResponse(Long userId, Long notificationId) {
        return notifications.findById(notificationId)
                .filter(n -> userId.equals(n.getUserId()))
                .map(n -> extractResponse(n.getPayloadJson()))
                .orElse(null);
    }

    /** Merge {@code response}/{@code respondedAt} into an existing payload JSON. */
    private String mergeResponse(String existingPayload, String verdict, Instant when) {
        Map<String, Object> root;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = existingPayload == null || existingPayload.isBlank()
                    ? Map.of()
                    : objectMapper.readValue(existingPayload, Map.class);
            root = new HashMap<>(parsed);
        } catch (JsonProcessingException ex) {
            // Existing payload is malformed JSON. Preserve it under a fallback
            // key so we don't drop it on the floor, then add our keys.
            root = new HashMap<>();
            root.put("_legacy", existingPayload);
        }
        root.put("response", verdict);
        root.put("respondedAt", when.toString());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            log.warn("mergeResponse: failed to serialise payload: {}", ex.getMessage());
            return existingPayload;
        }
    }

    /** Extract just the {@code response} key from a payload JSON, or null. */
    private String extractResponse(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(payloadJson, Map.class);
            Object v = parsed.get("response");
            return v == null ? null : v.toString();
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    // ----------------- internal -----------------

    /**
     * Look up the user's active device tokens, build a {@link NotificationRequest},
     * call the provider, and write one {@link NotificationEvent} row per token
     * with the right {@code status}/{@code errorCode}. Deactivates tokens
     * that come back PERMANENT_FAILURE.
     */
    private void dispatch(Long userId,
                          Long notificationId,
                          String type,
                          Map<String, String> titleByLang,
                          Map<String, String> bodyByLang,
                          String payloadJson) {
        List<DeviceToken> active = tokens.findAllByUserIdAndIsActive(userId, 1);
        if (active.isEmpty()) {
            NotificationEvent ev = newEvent(notificationId, NotificationEvent.Channel.PUSH);
            ev.setProvider(provider.name());
            ev.setStatus(NotificationEvent.Status.SKIPPED);
            ev.setErrorCode("NO_TOKEN");
            ev.setErrorMessage("user has no active device tokens");
            ev.setLastAttemptAt(Instant.now());
            events.save(ev);
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("nid", notificationId.toString());
        data.put("type", type);
        if (payloadJson != null && !payloadJson.isBlank()) {
            // FCM data values must be strings; store the raw JSON as one
            // string and let the client re-parse. Bounded by the controller.
            if (payloadJson.length() <= 4096) {
                data.put("payload", payloadJson);
            }
        }
        NotificationRequest req = NotificationRequest.builder()
                .type(type)
                .titleByLang(titleByLang)
                .bodyByLang(bodyByLang)
                .data(data)
                .tokens(active)
                .build();
        List<TokenResult> results;
        try {
            results = provider.send(req);
        } catch (Exception ex) {
            log.warn("provider {} threw: {}", provider.name(), ex.getMessage());
            results = active.stream()
                    .map(t -> TokenResult.retryable(t.getToken(), "PROVIDER_EXCEPTION",
                            ex.getClass().getSimpleName() + ": " + ex.getMessage()))
                    .toList();
        }
        Instant now = Instant.now();
        for (int i = 0; i < active.size(); i++) {
            DeviceToken dt = active.get(i);
            TokenResult r = i < results.size() ? results.get(i)
                    : TokenResult.retryable(dt.getToken(), "NO_RESULT", "provider returned no result");
            NotificationEvent ev = newEvent(notificationId, NotificationEvent.Channel.PUSH);
            ev.setProvider(provider.name());
            ev.setAttempts(1);
            ev.setLastAttemptAt(now);
            ev.setProviderMsgId(r.providerMsgId());
            ev.setErrorCode(r.errorCode());
            ev.setErrorMessage(r.errorMessage());
            switch (r.outcome()) {
                case SENT -> ev.setStatus(NotificationEvent.Status.SENT);
                case SKIPPED -> ev.setStatus(NotificationEvent.Status.SKIPPED);
                case PERMANENT_FAILURE, RETRYABLE -> ev.setStatus(NotificationEvent.Status.FAILED);
            }
            events.save(ev);
            if (r.outcome() == TokenResult.Outcome.PERMANENT_FAILURE) {
                deviceTokens.deactivateByToken(dt.getToken());
            }
        }
    }

    private static NotificationEvent newEvent(Long notificationId, NotificationEvent.Channel channel) {
        NotificationEvent ev = new NotificationEvent();
        ev.setNotificationId(notificationId);
        ev.setChannel(channel);
        return ev;
    }

    /** Fill missing languages from {@code messages.properties}. */
    private Map<String, String> mergeTitles(String type, Map<String, String> supplied) {
        Map<String, String> out = new HashMap<>();
        if (supplied != null) out.putAll(supplied);
        if (out.get("vi") == null) out.put("vi", messages.get("push." + type + ".title"));
        if (out.get("ko") == null) out.put("ko", messages.get("push." + type + ".title"));
        return out;
    }

    private Map<String, String> mergeBodies(String type, Map<String, String> supplied) {
        Map<String, String> out = new HashMap<>();
        if (supplied != null) out.putAll(supplied);
        if (out.get("vi") == null) out.put("vi", messages.get("push." + type + ".body"));
        if (out.get("ko") == null) out.put("ko", messages.get("push." + type + ".body"));
        return out;
    }

    /** Test/debug: convenient for assertions. */
    Optional<Notification> findByIdempotencyKey(String key) {
        return notifications.findFirstByIdempotencyKey(key);
    }

    /**
     * V2.3 / Phase E — Retry a single previously-FAILED push event.
     *
     * Called by {@link com.restaurant.server.service.NotificationRetryService}
     * after the exponential-backoff delay has elapsed. We look up the
     * original notification and re-attempt the push for whichever of the
     * user's device tokens is still active.
     *
     * Two outcomes:
     *   - The new provider call succeeds -> the FAILED row is left as a
     *     historical audit trail, and a fresh NotificationEvent row is
     *     appended with status=SENT.
     *   - The new provider call fails again -> a new NotificationEvent row
     *     is appended with status=FAILED, attempts incremented, and
     *     last_attempt_at updated so the next sweep considers it.
     *
     * Returns true when the retry was attempted (regardless of outcome);
     * false when the event doesn't exist or its underlying notification
     * has been deleted.
     */
    @Transactional
    public boolean retryOnce(NotificationEvent original) {
        if (original == null || original.getNotificationId() == null) return false;
        var n = notifications.findById(original.getNotificationId()).orElse(null);
        if (n == null) {
            log.warn("retryOnce: notification {} not found, skipping event {}",
                    original.getNotificationId(), original.getId());
            return false;
        }
        // Re-merge title/body through i18n so a future translation change
        // is picked up by the retried push.
        Map<String, String> titles = mergeTitles(n.getType(), Map.of(
                "vi", n.getTitleVi() == null ? "" : n.getTitleVi(),
                "ko", n.getTitleKo() == null ? "" : n.getTitleKo()));
        Map<String, String> bodies = mergeBodies(n.getType(), Map.of(
                "vi", n.getBodyVi() == null ? "" : n.getBodyVi(),
                "ko", n.getBodyKo() == null ? "" : n.getBodyKo()));
        dispatch(n.getUserId(), n.getId(), n.getType(), titles, bodies, n.getPayloadJson());
        return true;
    }

    @SuppressWarnings("unused")
    private static String lower(String s) { return s == null ? null : s.toLowerCase(Locale.ROOT); }
}
