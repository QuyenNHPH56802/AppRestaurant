-- V2.3 / V17: extend shift_assignments.status to 8 states
--
-- Existing lifecycle in V11 was:
--   SCHEDULED → CONFIRMED → COMPLETED | CANCELLED | SWAPPED
--
-- V2.3 adds staff-initiated transitions for the remote-shift flow:
--   ACCEPTED         — staff confirmed they will work the assigned shift
--   REJECTED         — staff declined (with `notes` reason)
--   CHANGE_REQUESTED — staff requested a different shift / time / zone
--
-- Full new lifecycle:
--   SCHEDULED  → CONFIRMED → ACCEPTED → COMPLETED
--                      ↘ REJECTED
--                      ↘ CHANGE_REQUESTED  → (manager re-assigns) → CONFIRMED
--   any state  → CANCELLED
--   CONFIRMED  → SWAPPED
--
-- New statuses do not conflict with existing ones; existing rows keep their
-- status. Application code is responsible for legality of the transition.
--
-- Implementation: SQLite does not support ALTER TABLE ... DROP CONSTRAINT,
-- so we use the same table-rebuild pattern as V6:
--   1. CREATE TABLE shift_assignments_new (...) with extended CHECK
--   2. INSERT INTO shift_assignments_new SELECT * FROM shift_assignments
--   3. DROP TABLE shift_assignments
--   4. ALTER TABLE shift_assignments_new RENAME TO shift_assignments
--   5. Recreate indexes
--
-- FK safeguard:
--   No other table currently REFERENCES shift_assignments (verified by grep
--   over V1-V16). The DROP is therefore safe with foreign_keys=ON.

CREATE TABLE shift_assignments_new (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shift_id        INTEGER NOT NULL,
    user_id         INTEGER NOT NULL,
    date            TEXT    NOT NULL CHECK (date LIKE '____-__-__'),
    status          TEXT    NOT NULL DEFAULT 'SCHEDULED'
                    CHECK (status IN ('SCHEDULED','CONFIRMED','ACCEPTED','REJECTED',
                                      'CHANGE_REQUESTED','COMPLETED','CANCELLED','SWAPPED')),
    notes           TEXT,
    approved_by     INTEGER,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (shift_id)    REFERENCES shifts(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id)     REFERENCES users(id)  ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id)  ON DELETE SET NULL
);

INSERT INTO shift_assignments_new
    (id, shift_id, user_id, date, status, notes, approved_by, created_at, updated_at)
SELECT
    id, shift_id, user_id, date, status, notes, approved_by, created_at, updated_at
FROM shift_assignments;

DROP TABLE shift_assignments;

ALTER TABLE shift_assignments_new RENAME TO shift_assignments;

CREATE INDEX ix_shift_assignments_shift    ON shift_assignments(shift_id);
CREATE INDEX ix_shift_assignments_user     ON shift_assignments(user_id);
CREATE INDEX ix_shift_assignments_date     ON shift_assignments(date);
CREATE INDEX ix_shift_assignments_status   ON shift_assignments(status);

CREATE UNIQUE INDEX ux_shift_assignments_one_per_day_per_shift
    ON shift_assignments(shift_id, user_id, date);
