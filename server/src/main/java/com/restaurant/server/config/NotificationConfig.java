package com.restaurant.server.config;

import com.restaurant.server.notify.FcmNotificationProvider;
import com.restaurant.server.notify.NoopNotificationProvider;
import com.restaurant.server.notify.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * V2.3 — Selects the active {@link NotificationProvider}.
 *
 * Default: {@link NoopNotificationProvider} (works offline, no Firebase key
 * required). When {@code restaurant.fcm.enabled=true}, a
 * {@link FcmNotificationProvider} bean is registered as {@code @Primary} so
 * Spring autowires it into {@code NotificationService}.
 *
 * Both beans coexist; the {@code @Primary} annotation picks the real one.
 * The noop remains in the context so admin/observability code can still
 * call {@code provider.name()} and get a deterministic answer.
 */
@Configuration
public class NotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Bean
    public NoopNotificationProvider noopNotificationProvider() {
        return new NoopNotificationProvider();
    }

    @Bean
    @Primary
    public NotificationProvider primaryNotificationProvider(
            RestaurantProperties props,
            NoopNotificationProvider noop,
            org.springframework.beans.factory.ObjectProvider<FcmNotificationProvider> fcmLazy) {
        boolean enabled = props.getFcm() != null && props.getFcm().isEnabled();
        if (enabled) {
            FcmNotificationProvider fcm = fcmLazy.getIfAvailable();
            if (fcm != null) {
                log.info("FCM push provider ENABLED (projectId='{}', dryRun={})",
                        props.getFcm().getProjectId(), props.getFcm().isDryRun());
                return fcm;
            }
        }
        log.info("FCM push provider DISABLED — using NoopNotificationProvider");
        return noop;
    }
}
