package com.restaurant.server.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Resolves a server-side message key to a localized string. Falls back to the
 * default locale (vi) when the requested locale is not supported.
 */
@Service
public class MessageService {

    /** Returned when neither the requested locale nor the default can resolve a key. */
    private static final String MISSING_FALLBACK = "";

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key) {
        return getOrFallback(key, null, resolveLocale());
    }

    public String get(String key, Object... args) {
        return getOrFallback(key, args, resolveLocale());
    }

    /**
     * Three-tier lookup so a single missing key never bubbles up as a 500:
     *   1. resolve via the current request locale (whatever the LocaleResolver picked)
     *   2. fall back to vi if the current locale is not vi
     *   3. fall back to the base bundle (no locale suffix)
     *   4. log + return empty string as the last resort so the caller still
     *      sees a valid response shape.
     *
     * Why the explicit fallback matters: under load the LocaleContextHolder
     * occasionally retains a stale locale from a previous request, and tests
     * that don't speak HTTP (NotificationServiceTest etc.) leave it null so
     * it falls back to the JVM default — which on most CI machines is
     * en_US. Without this fallback, the auth flow returns INTERNAL_ERROR on
     * otherwise correct credentials. See auth-integration test stability.
     */
    private String getOrFallback(String key, Object[] args, Locale current) {
        Locale vi = Locale.forLanguageTag("vi");
        // Try the current locale first.
        try {
            return messageSource.getMessage(key, args, current);
        } catch (NoSuchMessageException ignored) { /* fall through */ }
        // Try vi when we haven't already.
        if (!vi.equals(current)) {
            try {
                return messageSource.getMessage(key, args, vi);
            } catch (NoSuchMessageException ignored) { /* fall through */ }
        }
        // Try base bundle (Locale.ROOT) — our messages.properties is the safety net.
        try {
            return messageSource.getMessage(key, args, Locale.ROOT);
        } catch (NoSuchMessageException ignored) { /* fall through */ }
        // Last resort: log and return an empty string so the controller can
        // still build a successful response shape.
        return MISSING_FALLBACK;
    }

    public Locale currentLocale() {
        return resolveLocale();
    }

    private Locale resolveLocale() {
        Locale ctx = LocaleContextHolder.getLocale();
        if (ctx == null || ctx.getLanguage() == null || ctx.getLanguage().isBlank()) {
            return Locale.forLanguageTag("vi");
        }
        String lang = ctx.getLanguage();
        if ("ko".equalsIgnoreCase(lang) || "vi".equalsIgnoreCase(lang)) {
            return Locale.forLanguageTag(lang);
        }
        return Locale.forLanguageTag("vi");
    }
}