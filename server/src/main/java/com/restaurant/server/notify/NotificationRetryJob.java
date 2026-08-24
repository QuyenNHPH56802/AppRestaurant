package com.restaurant.server.notify;

import com.restaurant.server.service.NotificationRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * V2.3 / Phase E — Periodic retry sweep for failed push notifications.
 *
 * Runs every {@code restaurant.fcm.retry-interval-minutes} minutes
 * (default 5). One execution re-attempts up to
 * {@code restaurant.fcm.retry-max-per-sweep} events.
 *
 * The job is best-effort: an exception in one sweep must not stop the
 * next. We catch and log, same pattern as {@link DeviceTokenCleanupJob}.
 *
 * The job runs whether or not FCM is enabled. With the noop provider the
 * sweep still finds FAILED rows (because noop returns SKIPPED, not FAILED,
 * so the candidate set is empty in practice). With FCM enabled the sweep
 * provides the recovery loop for transient transport errors.
 */
@Component
public class NotificationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

    private final NotificationRetryService service;

    public NotificationRetryJob(NotificationRetryService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${restaurant.fcm.retry-interval-ms:300000}",
            initialDelayString = "${restaurant.fcm.retry-initial-delay-ms:60000}")
    public void scheduled() {
        try {
            int n = service.retryEligible();
            if (n > 0) log.info("notification retry sweep attempted {} events", n);
        } catch (Exception ex) {
            log.warn("notification retry sweep failed: {}", ex.getMessage());
        }
    }
}
