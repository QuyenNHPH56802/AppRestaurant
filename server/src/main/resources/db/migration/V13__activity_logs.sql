-- V2.2 / V13: activity_logs (business event log)
--
-- Centralized log for *business* events: login, logout, QR scan, check-in,
-- check-out, zone assignment, zone transfer, employee change, role change,
-- checklist completion, admin change, backup, restore, system errors.
--
-- This table runs parallel to the existing `audit_logs`. The split is
-- intentional:
--   - audit_logs: security-sensitive events (login, password change)
--   - activity_logs: business actions (check-in, transfer, etc.)
--
-- `metadata_json` is the per-event detail blob. Application code keeps it
-- under ~4 KB.
--
-- `result` is a CHECK for 'SUCCESS' or 'FAILURE' so dashboards can count
-- failures.
--
-- Both `actor_user_id` and `target_user_id` are SET NULL on user delete
-- to preserve history.

CREATE TABLE activity_logs (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_user_id     INTEGER,
    action            TEXT    NOT NULL,
    entity            TEXT,
    entity_id         TEXT,
    target_user_id    INTEGER,
    metadata_json     TEXT,
    ip                TEXT,
    user_agent        TEXT,
    result            TEXT    NOT NULL DEFAULT 'SUCCESS'
                      CHECK (result IN ('SUCCESS','FAILURE')),
    created_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (actor_user_id)  REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_activity_actor    ON activity_logs(actor_user_id);
CREATE INDEX ix_activity_action   ON activity_logs(action);
CREATE INDEX ix_activity_entity   ON activity_logs(entity, entity_id);
CREATE INDEX ix_activity_target   ON activity_logs(target_user_id);
CREATE INDEX ix_activity_result   ON activity_logs(result);
CREATE INDEX ix_activity_created  ON activity_logs(created_at);