package com.restaurant.server.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * V2.3 — Fallback provider used when {@code restaurant.fcm.enabled=false}
 * (the default in dev / on-prem deployments without internet).
 *
 * The {@link com.restaurant.server.service.NotificationService} still creates
 * a {@code notification_events} row with status=SKIPPED, so the audit trail
 * is complete: a developer looking at the admin dashboard sees the
 * notification WAS created, just not delivered. The {@code .send} payload
 * is logged at DEBUG level for offline inspection.
 */
@Component
public class NoopNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(NoopNotificationProvider.class);

    @Override
    public String name() { return "noop"; }

    @Override
    public List<TokenResult> send(NotificationRequest request) {
        log.debug("[noop] skipping push type={} tokens={} data={}",
                request.type(), request.tokens().size(), request.data());
        List<TokenResult> out = new ArrayList<>(request.tokens().size());
        for (var dt : request.tokens()) {
            out.add(TokenResult.skipped(dt.getToken(), "fcm disabled"));
        }
        return out;
    }

    @Override
    public boolean isReady() { return true; }
}
