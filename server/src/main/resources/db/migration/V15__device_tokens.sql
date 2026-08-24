-- V2.3 / V15: device_tokens
--
-- Purpose: persist FCM (and future APNs / WebPush) device tokens so the
-- NotificationService can push to every personal device an employee owns.
--
-- Design rules:
--   - One row per (user_id, token). Re-registering the same device on the
--     same phone is an idempotent upsert keyed on `token`.
--   - `is_active = 0` is the soft-disable state. We never DELETE rows; FCM
--     returns UNREGISTERED for invalid tokens, and we also flip this when
--     the user logs out OR when the cleanup job ages out a stale token.
--   - `last_seen_at` is updated on every successful push AND on every
--     register / re-register call, so we can detect abandoned devices.
--   - No PII (no IMEI, no MAC, no phone number). `device_id` is an opaque
--     stable identifier (ANDROID_ID on Android, identifierForVendor on iOS).
--   - Existing V1 + V2.2 data is not touched.
--
-- Push-side channels (e.g. 'FCM', 'APNS', 'WEB') live in V16 notification_events,
-- not here — a single physical device can switch channels.
--
-- Security note:
--   These tokens must NEVER be returned by an admin endpoint. The Admin API
--   exposes only an aggregate count per user.

CREATE TABLE device_tokens (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    token           TEXT    NOT NULL,
    platform        TEXT    NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
    device_id       TEXT,
    app_version     TEXT,
    last_seen_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    -- A given (user, token) pair is unique. Re-registering from the same
    -- device with the same FCM token is an upsert, not a duplicate row.
    UNIQUE (user_id, token)
);

-- Lookup by user — used by NotificationService.createAndDispatch
CREATE INDEX ix_device_tokens_user_active ON device_tokens(user_id, is_active);

-- Reverse lookup when FCM reports UNREGISTERED — used by FcmNotificationProvider
-- to deactivate a token without scanning the whole table.
CREATE INDEX ix_device_tokens_token       ON device_tokens(token);

-- Cleanup job will age out devices last-seen > 180 days.
CREATE INDEX ix_device_tokens_last_seen   ON device_tokens(last_seen_at);
