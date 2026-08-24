package com.restaurant.server.notify;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.service.DeviceTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * V2.3 — Daily cleanup of stale device tokens.
 *
 * Runs at 03:00 server time (configurable via {@code restaurant.fcm.cleanup-cron}).
 * A token is considered stale when {@code lastSeenAt} is older than
 * {@code restaurant.fcm.staleAfterDays} (default 180). The job DEACTIVATES
 * (does NOT delete) so the row remains as a record — the FCM provider
 * will keep receiving the deactivated row only if the device somehow
 * returns, and our register path idempotently re-activates it.
 *
 * Failure handling: a single bad day must not stop the next day. We catch
 * and log; the scheduler will retry at the next cron tick.
 */
@Component
public class DeviceTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenCleanupJob.class);

    private final DeviceTokenService service;
    private final RestaurantProperties props;

    public DeviceTokenCleanupJob(DeviceTokenService service, RestaurantProperties props) {
        this.service = service;
        this.props = props;
    }

    @Scheduled(cron = "${restaurant.fcm.cleanup-cron:0 0 3 * * *}",
               zone = "${restaurant.fcm.cleanup-zone:Asia/Ho_Chi_Minh}")
    public void scheduled() {
        try {
            int n = service.deactivateStaleTokens(props.getFcm().getStaleAfterDays());
            if (n > 0) log.info("stale-token cleanup deactivated {} tokens", n);
        } catch (Exception ex) {
            log.warn("stale-token cleanup failed: {}", ex.getMessage());
        }
    }
}
