package com.restaurant.server.service;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.NotificationEvent;
import com.restaurant.server.repository.NotificationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * V2.3 / Phase E — Retries push notifications that ended in
 * {@link NotificationEvent.Status#FAILED} on the previous attempt.
 *
 * <h2>Why</h2>
 * The FCM transport returns RETRYABLE outcomes for transient errors
 * (HTTP 5xx, {@code UNAVAILABLE}, {@code INTERNAL}, {@code QUOTA_EXCEEDED}).
 * A retry job is needed because the request path completes synchronously
 * — there's no background queue — so transient failures would otherwise
 * stay unrecovered until a user re-triggers the business event.
 *
 * <h2>How</h2>
 * A scheduled sweep (see {@code NotificationRetryJob}) invokes
 * {@link #retryEligible()} every {@code restaurant.fcm.retry-interval}
 * minutes. The sweep:
 *
 * <ol>
 *   <li>Finds FAILED PUSH events with {@code attempts < maxAttempts} and
 *       {@code lastAttemptAt < now() - backoff(attempts)}.</li>
 *   <li>For each candidate, delegates to
 *       {@link NotificationService#retryOnce(NotificationEvent)} which
 *       re-runs {@code NotificationService.dispatch} with the latest
 *       device tokens and translations.</li>
 *   <li>Stops after {@code maxPerSweep} to keep the job bounded and
 *       protect against large backlogs dominating CPU.</li>
 * </ol>
 *
 * <h2>Backoff schedule</h2>
 * Deterministic per {@code attempts} count:
 * <pre>
 *   attempts=1 -&gt; wait 1 min
 *   attempts=2 -&gt; wait 5 min
 *   attempts=3 -&gt; wait 30 min
 *   attempts=4 -&gt; wait 2 h  (only if maxAttempts&gt;=5)
 *   attempts=5 -&gt; wait 12 h (only if maxAttempts&gt;=6)
 * </pre>
 * After {@code maxAttempts} the event is permanently FAILED and the row
 * stays in the audit trail.
 *
 * <h2>Threading</h2>
 * Single-threaded (Spring's default TaskScheduler). The job is best-effort:
 * if two replicas of the server run, both may retry the same event in
 * parallel; that's safe — the {@code NotificationService.dispatch} path
 * is idempotent at the {@code (user_id, device_token, type)} level thanks
 * to V18's idempotency key and the FCM transport's own at-most-once
 * semantics per token.
 */
@Service
public class NotificationRetryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryService.class);

    /** Backoff in minutes indexed by current {@code attempts} count. */
    private static final int[] BACKOFF_MINUTES = { 1, 5, 30, 120, 720 };

    private final NotificationEventRepository events;
    private final NotificationService notifications;
    private final RestaurantProperties props;

    public NotificationRetryService(NotificationEventRepository events,
                                   NotificationService notifications,
                                   RestaurantProperties props) {
        this.events = events;
        this.notifications = notifications;
        this.props = props;
    }

    /**
     * Run one sweep of the retry queue. Returns the number of events
     * successfully re-attempted (regardless of whether each succeeded —
     * a re-attempt that ends in another FAILED is still "attempted").
     */
    @Transactional
    public int retryEligible() {
        int maxAttempts = Math.max(1, props.getFcm().getRetryMaxAttempts());
        int maxPerSweep = Math.max(1, props.getFcm().getRetryMaxPerSweep());
        Instant now = Instant.now();

        // Pre-filter at the DB layer: only events whose last attempt was
        // older than the smallest backoff bucket (1 min). Anything more
        // recent is filtered out anyway by the per-event backoff check,
        // but skipping them at the DB keeps the sweep bounded on busy days.
        Instant since = now.minus(Duration.ofMinutes(backoffMinutesFor(0)));
        List<NotificationEvent> candidates = events.findRetryCandidates(
                NotificationEvent.Channel.PUSH,
                NotificationEvent.Status.FAILED,
                maxAttempts,
                since);

        int attempted = 0;
        for (NotificationEvent ev : candidates) {
            if (attempted >= maxPerSweep) break;
            int attempts = ev.getAttempts() == null ? 0 : ev.getAttempts();
            if (attempts >= maxAttempts) continue;
            int backoffMin = backoffMinutesFor(attempts);
            Instant eligibleAt = (ev.getLastAttemptAt() == null
                    ? ev.getCreatedAt()
                    : ev.getLastAttemptAt()).plus(Duration.ofMinutes(backoffMin));
            if (eligibleAt.isAfter(now)) continue;

            try {
                boolean ok = notifications.retryOnce(ev);
                if (ok) {
                    attempted++;
                    log.debug("retry event {} (notification {}, attempts {})",
                            ev.getId(), ev.getNotificationId(), attempts);
                }
            } catch (Exception ex) {
                log.warn("retry event {} threw: {}", ev.getId(), ex.getMessage());
            }
        }
        return attempted;
    }

    /**
     * Backoff lookup. Exposed as static so tests can assert the schedule
     * without spinning up the whole Spring context.
     */
    static int backoffMinutesFor(int attempts) {
        if (attempts < 0) attempts = 0;
        if (attempts >= BACKOFF_MINUTES.length) return BACKOFF_MINUTES[BACKOFF_MINUTES.length - 1];
        return BACKOFF_MINUTES[attempts];
    }
}
