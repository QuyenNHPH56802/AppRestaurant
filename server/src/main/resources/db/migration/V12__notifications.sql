-- V2.2 / V12: notifications
--
-- In-app notification feed for a user. Each row targets a single user.
-- title_vi / title_ko / body_vi / body_ko are inline so notifications
-- render correctly regardless of locale changes.
--
-- `type` is a free-form varchar with a documented allow-list (no DB CHECK
-- because types will grow over time). Application code validates via enum.
--
-- `payload_json` is a string blob for type-specific data (e.g. zone_id,
-- shift_id, checklist_id). Kept as TEXT for SQLite simplicity; Hibernate
-- reads it as String.
--
-- `read_at` is NULL until the user reads the notification. A periodic
-- cleanup job (separate from migrations) can archive old notifications.
--
-- Future push:
--   This table is the *in-app* channel. A future V3 push channel can
--   subscribe to the same create-event without modifying this schema.

CREATE TABLE notifications (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    type            TEXT    NOT NULL,
    title_vi        TEXT    NOT NULL,
    title_ko        TEXT    NOT NULL,
    body_vi         TEXT,
    body_ko         TEXT,
    payload_json    TEXT,
    read_at         TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX ix_notifications_user         ON notifications(user_id);
CREATE INDEX ix_notifications_user_unread  ON notifications(user_id, read_at);
CREATE INDEX ix_notifications_type         ON notifications(type);
CREATE INDEX ix_notifications_created     ON notifications(created_at);