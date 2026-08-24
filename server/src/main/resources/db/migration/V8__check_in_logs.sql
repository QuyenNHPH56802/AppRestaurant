-- V2.2 / V8: check_in_logs
--
-- Records every check-in / check-out event. Read-only once written.
-- Business rules enforced by server (not by CHECK) in V2.7:
--   - cannot check into a DISABLED zone
--   - cannot have two open check-ins for the same user at the same time
--   - cannot check out without an open check-in
--
-- Columns:
--   - user_id    SET NULL on user delete (preserve event history)
--   - zone_id    RESTRICT (a zone cannot be hard-deleted while check-ins exist;
--                use status='DISABLED' instead)
--   - action     CHECK ('CHECK_IN','CHECK_OUT')
--   - client_ip  for security audit (not for analytics)
--   - device_id  optional; nullable for kiosk devices that don't expose an ID
--   - notes      optional free-text (e.g. "ended early - emergency")

CREATE TABLE check_in_logs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    zone_id         INTEGER NOT NULL,
    action          TEXT    NOT NULL CHECK (action IN ('CHECK_IN','CHECK_OUT')),
    notes           TEXT,
    device_id       TEXT,
    client_ip       TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE RESTRICT
);
CREATE INDEX ix_check_in_user     ON check_in_logs(user_id);
CREATE INDEX ix_check_in_zone     ON check_in_logs(zone_id);
CREATE INDEX ix_check_in_action   ON check_in_logs(action);
CREATE INDEX ix_check_in_created  ON check_in_logs(created_at);

-- Composite index: "current check-in per user" query
-- (WHERE user_id = ? AND action = 'CHECK_IN' ORDER BY created_at DESC LIMIT 1)
CREATE INDEX ix_check_in_user_action_created ON check_in_logs(user_id, action, created_at);