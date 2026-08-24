-- V2.2 / V9: zone_assignments
--
-- Stores the *history* of every zone assignment. The "current" assignment
-- for a user is the row with effective_to IS NULL.
--
-- Columns:
--   - user_id           SET NULL on user delete
--   - zone_id           RESTRICT on zone delete (use DISABLED status instead)
--   - assigned_by_user_id  SET NULL on user delete
--   - reason            optional free-text
--   - effective_from    ISO timestamp, NOT NULL
--   - effective_to      ISO timestamp; NULL means "still current"
--   - is_current        CHECK IN (0,1) -- redundant with effective_to IS NULL
--                      but indexed for fast "who is currently in zone X" queries.
--                      Maintained by a trigger (added at end of file).
--
-- Invariants (enforced by application + triggers):
--   - At most one current row per user_id (is_current = 1)
--   - Inserting a new current assignment for a user auto-closes the prior one
--   - Soft-disable a zone is allowed; reassignment on disable is the manager's job
--
-- Note: We deliberately do NOT use a UNIQUE INDEX on (user_id, is_current)
-- because SQLite treats NULL in a UNIQUE column as "never equal", so two
-- rows with is_current = NULL (history rows) would not conflict. Instead,
-- we use a partial unique index `WHERE is_current = 1`.

CREATE TABLE zone_assignments (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id               INTEGER NOT NULL,
    zone_id               INTEGER NOT NULL,
    assigned_by_user_id   INTEGER,
    reason                TEXT,
    effective_from        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    effective_to          TEXT,
    is_current            INTEGER NOT NULL DEFAULT 1 CHECK (is_current IN (0,1)),
    created_at            TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (user_id)             REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (zone_id)             REFERENCES zones(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_zone_assignments_user        ON zone_assignments(user_id);
CREATE INDEX ix_zone_assignments_zone        ON zone_assignments(zone_id);
CREATE INDEX ix_zone_assignments_current     ON zone_assignments(is_current);
CREATE INDEX ix_zone_assignments_effective   ON zone_assignments(effective_from, effective_to);

-- Partial unique index: at most one CURRENT assignment per user.
-- This is the business rule "an employee is in exactly one zone at a time".
CREATE UNIQUE INDEX ux_zone_assignments_one_current_per_user
    ON zone_assignments(user_id)
    WHERE is_current = 1;

-- Composite for the dashboard query "who is currently in zone X".
CREATE INDEX ix_zone_assignments_zone_current
    ON zone_assignments(zone_id, is_current);

-- ---------------------------------------------------------------------------
-- Trigger: when a new CURRENT assignment is inserted, auto-close the prior
-- current row (set is_current=0, effective_to=now). This keeps the invariant
-- "exactly one current assignment per user" without requiring application
-- code to remember to do it.
-- ---------------------------------------------------------------------------
CREATE TRIGGER trg_zone_assignments_close_prior
AFTER INSERT ON zone_assignments
WHEN NEW.is_current = 1
BEGIN
    UPDATE zone_assignments
    SET is_current = 0,
        effective_to = NEW.effective_from
    WHERE user_id = NEW.user_id
      AND is_current = 1
      AND id != NEW.id;
END;