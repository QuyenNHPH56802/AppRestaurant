package com.restaurant.server.notify;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.DeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * V2.3 — Real FCM push delivery via firebase-admin.
 *
 * Activation: this bean is created ONLY when {@code restaurant.fcm.enabled=true}
 * (see {@code NotificationConfig}). When the flag is false, the noop provider is
 * primary and this class is never instantiated.
 *
 * Lazy init: {@link #ensureApp()} only initialises {@link FirebaseApp} on the
 * first {@link #send} call. We never want to fail-fast at startup — a missing
 * or malformed service-account JSON should let the server boot (so the admin
 * panel still works) and just log a warning + fall back to noop. After the
 * missing file is fixed, a single restart re-enables real delivery.
 *
 * Batching: FCM HTTP v1 caps MulticastMessage at 500 tokens. The provider
 * here always targets a single user (typically 1-3 devices), so 500 is never
 * hit. We still chunk defensively in case a future caller passes a
 * multi-recipient list.
 *
 * Error mapping:
 *   - {@code UNREGISTERED} / {@code INVALID_ARGUMENT} / {@code SENDER_ID_MISMATCH}
 *     -> PERMANENT_FAILURE (caller deactivates the token)
 *   - {@code INTERNAL} / {@code UNAVAILABLE} / {@code QUOTA_EXCEEDED} -> RETRYABLE
 *   - all else -> RETRYABLE with a sanitised error message
 *
 * Security: error messages are scrubbed of any string that looks like an FCM
 * token (long base64url-shaped substrings) before they leave this class.
 */
@Component
public class FcmNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationProvider.class);
    /** FCM HTTP v1 hard limit. */
    private static final int MAX_BATCH = 500;

    private final RestaurantProperties props;
    private volatile boolean initAttempted = false;
    private volatile FirebaseApp app;
    /** For tests: defaults to {@code FirebaseMessaging::getInstance}. */
    private volatile java.util.function.Function<FirebaseApp, FirebaseMessaging> messagingLookup =
            FirebaseMessaging::getInstance;

    public FcmNotificationProvider(RestaurantProperties props) {
        this.props = props;
    }

    @Override
    public String name() { return "fcm"; }

    @Override
    public boolean isReady() {
        if (app != null) return true;
        if (!initAttempted) {
            try { ensureApp(); } catch (Exception ignored) { /* logged inside */ }
        }
        return app != null;
    }

    @Override
    public List<TokenResult> send(NotificationRequest request) {
        try {
            ensureApp();
        } catch (Exception ex) {
            log.warn("FCM not initialised; falling back to SKIPPED. cause={}", ex.getMessage());
            return skipAll(request, "fcm not initialised: " + ex.getMessage());
        }
        if (app == null) {
            return skipAll(request, "fcm not initialised");
        }

        // dryRun: log payload, never call Firebase. Useful for dev / QA.
        if (props.getFcm().isDryRun()) {
            log.info("[fcm:dryRun] type={} tokens={} data={} titles={}",
                    request.type(), request.tokens().size(),
                    request.data(), request.titleByLang());
            List<TokenResult> out = new ArrayList<>(request.tokens().size());
            for (var t : request.tokens()) out.add(TokenResult.skipped(t.getToken(), "dryRun"));
            return out;
        }

        List<TokenResult> all = new ArrayList<>(request.tokens().size());
        List<DeviceToken> tokens = request.tokens();
        for (int i = 0; i < tokens.size(); i += MAX_BATCH) {
            List<DeviceToken> chunk = tokens.subList(i, Math.min(i + MAX_BATCH, tokens.size()));
            all.addAll(sendChunk(request, chunk));
        }
        return all;
    }

    private List<TokenResult> sendChunk(NotificationRequest request, List<DeviceToken> chunk) {
        MulticastMessage.Builder mb = MulticastMessage.builder()
                .addAllTokens(chunk.stream().map(DeviceToken::getToken).toList())
                .putAllData(request.data())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(pickTitle(request))
                        .setBody(pickBody(request))
                        .build());

        // Per-platform config so the OS-level channel + icon are right.
        // Android uses default channel; iOS uses alert + sound; WebPush
        // piggybacks the same title/body.
        AndroidConfig android = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
        mb.setAndroidConfig(android);
        ApnsConfig apns = ApnsConfig.builder()
                .setAps(Aps.builder().setSound("default").build())
                .build();
        mb.setApnsConfig(apns);
        WebpushConfig webpush = WebpushConfig.builder()
                .setNotification(new WebpushNotification(pickTitle(request), pickBody(request)))
                .build();
        mb.setWebpushConfig(webpush);

        try {
            // firebase-admin 9.x: sendEachForMulticastAsync returns ApiFuture<BatchResponse>
            // We use the blocking variant with timeout — async-ok but simpler
            // synchronous path is fine for our throughput.
            ApiFuture<BatchResponse> future = messagingLookup.apply(app)
                    .sendEachForMulticastAsync(mb.build());
            BatchResponse resp = future.get(15, TimeUnit.SECONDS);
            return toResults(chunk, resp);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return skipAll(new NotificationRequest(request.type(),
                    request.titleByLang(), request.bodyByLang(), request.data(), chunk),
                    "interrupted");
        } catch (ExecutionException | TimeoutException ex) {
            log.warn("FCM batch send transport error: {}", ex.getMessage());
            List<TokenResult> out = new ArrayList<>(chunk.size());
            for (DeviceToken t : chunk) {
                out.add(TokenResult.retryable(t.getToken(), "TRANSPORT", scrub(ex.getMessage())));
            }
            return out;
        } catch (RuntimeException ex) {
            // Defensive: a misbehaving transport mock or an unexpected SDK
            // exception must NOT propagate out of send() — the caller
            // (NotificationService) would record nothing and the user
            // would silently miss the notification.
            log.warn("FCM batch send threw {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            List<TokenResult> out = new ArrayList<>(chunk.size());
            for (DeviceToken t : chunk) {
                out.add(TokenResult.retryable(t.getToken(), "TRANSPORT",
                        ex.getClass().getSimpleName() + ": " + scrub(ex.getMessage())));
            }
            return out;
        }
    }

    private List<TokenResult> toResults(List<DeviceToken> chunk, BatchResponse resp) {
        List<TokenResult> out = new ArrayList<>(chunk.size());
        var responses = resp.getResponses();
        for (int i = 0; i < chunk.size(); i++) {
            DeviceToken dt = chunk.get(i);
            var r = responses.get(i);
            if (r.isSuccessful()) {
                out.add(TokenResult.sent(dt.getToken(), r.getMessageId()));
            } else {
                FirebaseMessagingException ex = r.getException();
                out.add(mapError(dt.getToken(),
                        ex == null ? null : ex.getMessagingErrorCode(),
                        ex == null ? "unknown" : ex.getMessage()));
            }
        }
        return out;
    }

    private TokenResult mapError(String token, MessagingErrorCode code, String rawMessage) {
        String codeStr = code == null ? "UNKNOWN" : code.name();
        String scrubbed = scrub(rawMessage);
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.SENDER_ID_MISMATCH
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR) {
            return TokenResult.permanent(token, codeStr, scrubbed);
        }
        return TokenResult.retryable(token, codeStr, scrubbed);
    }

    /**
     * Best-effort scrub: strip anything that looks like an FCM token out of
     * the error message before it lands in the DB. Tokens are 140+ chars of
     * base64url with optional colons and dashes; a length+charset heuristic
     * is enough — we are not building a parser.
     */
    static String scrub(String message) {
        if (message == null) return null;
        // Drop anything 64+ chars of base64url-looking content. Conservative.
        return message.replaceAll("[A-Za-z0-9_\\-:]{64,}", "<token-redacted>");
    }

    private static String pickTitle(NotificationRequest r) {
        return r.titleByLang().getOrDefault("vi",
                r.titleByLang().values().stream().findFirst().orElse(""));
    }

    private static String pickBody(NotificationRequest r) {
        return r.bodyByLang().getOrDefault("vi",
                r.bodyByLang().values().stream().findFirst().orElse(""));
    }

    private static List<TokenResult> skipAll(NotificationRequest r, String reason) {
        List<TokenResult> out = new ArrayList<>(r.tokens().size());
        for (var t : r.tokens()) out.add(TokenResult.skipped(t.getToken(), reason));
        return out;
    }

    /**
     * Idempotent: the first call wires FirebaseApp; subsequent calls are
     * no-ops. If the service-account file is missing the first call returns
     * a clean state ({@code app == null}) — caller falls back to SKIPPED.
     */
    private synchronized void ensureApp() throws IOException {
        if (app != null) return;
        if (initAttempted && app == null) return;
        initAttempted = true;
        Path credPath = resolveCredentials();
        if (credPath == null || !Files.isReadable(credPath)) {
            log.warn("FCM credentials not readable at '{}'; push disabled",
                    credPath == null ? "<null>" : credPath);
            return;
        }
        try (FileInputStream in = new FileInputStream(credPath.toFile())) {
            GoogleCredentials creds = GoogleCredentials.fromStream(in);
            String projectId = props.getFcm().getProjectId();
            FirebaseOptions opts = FirebaseOptions.builder()
                    .setCredentials(creds)
                    .setProjectId(projectId == null || projectId.isBlank() ? null : projectId)
                    .build();
            // Avoid double-init on hot-reload
            for (FirebaseApp existing : FirebaseApp.getApps()) {
                if (existing.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    this.app = existing;
                    return;
                }
            }
            this.app = FirebaseApp.initializeApp(opts, FirebaseApp.DEFAULT_APP_NAME);
            log.info("FirebaseApp initialised: name={} projectId={}",
                    app.getName(), projectId);
        }
    }

    private Path resolveCredentials() {
        String configured = props.getFcm().getCredentialsPath();
        if (configured == null || configured.isBlank()) return null;
        Path p = Paths.get(configured);
        if (Files.isReadable(p)) return p;
        // Fall back to <configDir>/<filename>
        String cfg = props.getConfigDir();
        if (cfg != null && !cfg.isBlank()) {
            Path alt = Paths.get(cfg, configured);
            if (Files.isReadable(alt)) return alt;
        }
        // Also try with a literal <configDir>/firebase-service-account.json when
        // the configured path is just a basename
        if (!configured.contains("/") && !configured.contains("\\")) {
            if (cfg != null && !cfg.isBlank()) {
                return Paths.get(cfg, configured);
            }
        }
        return p;
    }

    /** For tests. */
    void resetForTest() {
        this.app = null;
        this.initAttempted = false;
    }

    /** For tests: inject a pre-built {@link FirebaseApp} so we can exercise the
     *  send path without real credentials. The next {@link #send} call uses
     *  this app directly and skips {@link #ensureApp()}. */
    void setAppForTest(FirebaseApp app) {
        this.app = app;
        this.initAttempted = true;
    }

    /**
     * For tests: substitute the static {@link FirebaseMessaging#getInstance} call.
     * Accepts a function that, given an app, returns the messaging instance
     * to use. Pass {@code null} to revert to the default static lookup.
     */
    void setMessagingLookupForTest(java.util.function.Function<FirebaseApp, FirebaseMessaging> lookup) {
        this.messagingLookup = lookup;
    }

    /** Defensive: in case a future caller mis-spells. */
    @SuppressWarnings("unused")
    private static String lower(String s) { return s == null ? null : s.toLowerCase(Locale.ROOT); }
}
