-- V2.2 / V11: shifts + shift_assignments
--
-- A shift defines a working window (start_time + end_time as local TIME).
-- shift_assignments binds a user to a specific date inside a shift.
--
-- Design:
--   - `date` is ISO date string ('YYYY-MM-DD') rather than epoch days to make
--     SQL queries human-readable. Application code is responsible for
--     validating format.
--   - `status` lifecycle: SCHEDULED -> CONFIRMED -> COMPLETED | CANCELLED |
--     SWAPPED. SWAPPED means the user requested a swap; the record is kept
--     for history. A new assignment is then created for the swap target.
--   - No drag-drop UI in v1; weekly grid (MANAGER view) reads these rows
--     and renders them as a simple table.
--
-- Time zone:
--   - All TIMEs are stored as local 'HH:MM' strings. Asia/Ho_Chi_Minh is
--     the server's default. The shift records the timezone in the `tz`
--     column so multi-location deployments can coexist later.

CREATE TABLE shifts (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    description     TEXT,
    start_time      TEXT    NOT NULL CHECK (start_time LIKE '__:__'),
    end_time        TEXT    NOT NULL CHECK (end_time   LIKE '__:__'),
    tz              TEXT    NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    CHECK (end_time <> start_time)
);

CREATE INDEX ix_shifts_active ON shifts(is_active);

CREATE TABLE shift_assignments (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shift_id        INTEGER NOT NULL,
    user_id         INTEGER NOT NULL,
    date            TEXT    NOT NULL CHECK (date LIKE '____-__-__'),
    status          TEXT    NOT NULL DEFAULT 'SCHEDULED'
                    CHECK (status IN ('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED','SWAPPED')),
    notes           TEXT,
    approved_by     INTEGER,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (shift_id)    REFERENCES shifts(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id)     REFERENCES users(id)  ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id)  ON DELETE SET NULL
);
CREATE INDEX ix_shift_assignments_shift    ON shift_assignments(shift_id);
CREATE INDEX ix_shift_assignments_user     ON shift_assignments(user_id);
CREATE INDEX ix_shift_assignments_date     ON shift_assignments(date);
CREATE INDEX ix_shift_assignments_status   ON shift_assignments(status);

-- A user can be assigned to multiple shifts per day, but not twice to the
-- same shift on the same day.
CREATE UNIQUE INDEX ux_shift_assignments_one_per_day_per_shift
    ON shift_assignments(shift_id, user_id, date);

-- ---------------------------------------------------------------------------
-- Seed: 3 default shifts (Morning / Afternoon / Evening) so the calendar is
-- not empty on first run. Configurable in V2.12.
-- ---------------------------------------------------------------------------
INSERT OR IGNORE INTO shifts (id, name, description, start_time, end_time, sort_order) VALUES
    (1, 'Ca sáng',   'Ca làm việc buổi sáng',   '06:00', '14:00', 1),
    (2, 'Ca chiều',  'Ca làm việc buổi chiều',  '14:00', '22:00', 2),
    (3, 'Ca tối',    'Ca làm việc buổi tối',    '22:00', '06:00', 3);