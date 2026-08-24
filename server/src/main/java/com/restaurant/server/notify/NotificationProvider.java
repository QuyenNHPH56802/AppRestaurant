package com.restaurant.server.notify;

import java.util.List;

/**
 * V2.3 — Abstraction over a push-notification transport.
 *
 * Why an interface: the project ships with two implementations:
 *   - {@link NoopNotificationProvider} — logs and returns SKIPPED. Default
 *     when {@code restaurant.fcm.enabled=false}.
 *   - {@link FcmNotificationProvider} — uses firebase-admin. Active when
 *     {@code restaurant.fcm.enabled=true} and the service-account JSON is
 *     reachable.
 *
 * NotificationService selects exactly one bean via Spring's {@code @Primary}
 * resolution. We never instantiate both at once; the {@code NotificationConfig}
 * wiring handles the conditional.
 *
 * Threading: implementations MUST be safe to call from any thread, including
 * concurrent calls from the scheduled job and a request thread.
 */
public interface NotificationProvider {

    /**
     * @return short stable identifier (e.g. "fcm", "noop", "apns"). Stored in
     *         {@code notification_events.provider} for observability.
     */
    String name();

    /**
     * Send the same payload to many tokens in one provider call (FCM supports
     * up to 500 tokens per request — batched here so we never split a
     * notification into multiple provider messages).
     *
     * @param request the per-user payload + data + token list
     * @return a {@link SendResult} per token, in the same order. Implementations
     *         must NEVER return null entries.
     */
    List<TokenResult> send(NotificationRequest request);

    /** Marker: is the underlying transport wired and healthy? */
    boolean isReady();
}
