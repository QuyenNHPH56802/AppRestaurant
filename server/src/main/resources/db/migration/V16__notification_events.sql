-- V2.3 / V16: notification_events
--
-- Purpose: trace every attempted delivery of a notification row through every
-- channel (PUSH / IN_APP / future EMAIL / SMS). One row per (notification, channel)
-- is the natural design; we keep it explicit so the dashboard can show
-- "sent / failed / pending" without scanning the application logs.
--
-- Why a separate table (instead of columns on `notifications`):
--   - A notification may have multiple events (e.g. PUSH + IN_APP).
--   - FCM retries are tracked by `attempts` rather than new rows.
--   - Idempotency: NotificationService creates ONE event per (notification, channel)
--     and updates its status; a duplicate FCM delivery does not duplicate events.
--   - Mirrors the architectural note in V12: the in-app channel and the push
--     channel are siblings, not nested.
--
-- Status lifecycle:
--   PENDING  → SENT     — push delivered, got FCM message name back
--   PENDING  → FAILED   — provider error, will retry on next event OR after backoff
--   PENDING  → SKIPPED  — provider disabled (e.g. fcm.enabled=false in dev)
--
-- Security:
--   `provider_msg_id` is FCM's "projects/.../messages/<id>" — safe to log.
--   `error_message` is the FCM error string — may contain the token. We DELIBERATELY
--   do not store the token here; it lives only in device_tokens.

CREATE TABLE notification_events (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    notification_id   INTEGER NOT NULL,
    channel           TEXT    NOT NULL CHECK (channel IN ('PUSH','IN_APP','EMAIL')),
    status            TEXT    NOT NULL CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED')),
    provider          TEXT,                                                  -- 'fcm' | 'noop' | 'apns' ...
    provider_msg_id   TEXT,                                                  -- e.g. FCM message name
    error_code        TEXT,                                                  -- e.g. 'UNREGISTERED'
    error_message     TEXT,                                                  -- short error string, never the token
    attempts          INTEGER NOT NULL DEFAULT 0,
    last_attempt_at   TEXT,
    created_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Look-up by notification — shows the dashboard "what happened with this push"
CREATE INDEX ix_notification_events_notif  ON notification_events(notification_id);

-- Look-up by status — used by the cleanup job to expire PENDING rows > N hours
CREATE INDEX ix_notification_events_status ON notification_events(status);

-- Look-up by channel + created_at — used by the admin "recent push events" view
CREATE INDEX ix_notification_events_channel_created
    ON notification_events(channel, created_at);
