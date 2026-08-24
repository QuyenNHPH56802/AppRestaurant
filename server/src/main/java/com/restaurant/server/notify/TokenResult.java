package com.restaurant.server.notify;

/**
 * V2.3 — Per-token delivery outcome.
 *
 * One {@link TokenResult} per device token in the same order as the request.
 * The provider's job is to map its own error envelopes to one of the
 * {@link Outcome} values — {@code NotificationService} does not interpret
 * provider-specific error codes.
 *
 * SECURITY: {@code errorMessage} is written to {@code notification_events.error_message}
 * after a sanitiser strips tokens. Providers MUST NOT include the raw FCM token
 * in this string; FCM's error JSON can echo it back.
 */
public record TokenResult(String token, Outcome outcome, String providerMsgId,
                          String errorCode, String errorMessage) {

    public enum Outcome {
        /** Provider returned 200 with a message id. */
        SENT,
        /** Provider returned an error that may be transient (5xx, timeout). */
        RETRYABLE,
        /** Provider returned an error that will never succeed (UNREGISTERED,
         *  invalid token, sender mismatch). Token will be deactivated. */
        PERMANENT_FAILURE,
        /** Provider was disabled (e.g. fcm.enabled=false). Token is not at fault. */
        SKIPPED
    }

    public static TokenResult sent(String token, String providerMsgId) {
        return new TokenResult(token, Outcome.SENT, providerMsgId, null, null);
    }

    public static TokenResult retryable(String token, String code, String message) {
        return new TokenResult(token, Outcome.RETRYABLE, null, code, message);
    }

    public static TokenResult permanent(String token, String code, String message) {
        return new TokenResult(token, Outcome.PERMANENT_FAILURE, null, code, message);
    }

    public static TokenResult skipped(String token, String reason) {
        return new TokenResult(token, Outcome.SKIPPED, null, "SKIPPED", reason);
    }
}
