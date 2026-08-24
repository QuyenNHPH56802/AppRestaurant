-- V2.2 / V10: checklists, checklist_translations, checklist_tasks,
--               checklist_task_translations, checklist_completions
--
-- A checklist is a *zone-scoped* list of tasks that staff complete during a
-- shift. Each task can be required or optional.
--
-- Translations follow the V1 pattern (separate *_translations table per
-- content table) so future languages (en, ja, zh) can be added without a
-- schema change.
--
-- Completions:
--   - One row per task completion. A staff member can re-complete a task
--     (e.g. daily checklist) which produces another row.
--   - `status` is 'COMPLETED' or 'SKIPPED' (only allowed if task is optional).
--   - Photo upload is optional and gated by permission CHECKLIST_UPLOAD_PHOTO.
--   - shift_id is nullable so a completion can happen outside a shift (e.g.
--     before the day's shift is created).
--
-- Note: we deliberately do NOT soft-delete checklist_tasks. Deleting a task
-- that already has completions would orphan history. Use is_active=0 instead.

CREATE TABLE checklists (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    zone_id         INTEGER NOT NULL,
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE RESTRICT
);
CREATE INDEX ix_checklists_zone    ON checklists(zone_id);
CREATE INDEX ix_checklists_active  ON checklists(is_active);

CREATE TABLE checklist_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    checklist_id    INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    title           TEXT    NOT NULL,
    description     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (checklist_id) REFERENCES checklists(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_checklist_translation ON checklist_translations(checklist_id, language_code);

CREATE TABLE checklist_tasks (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    checklist_id    INTEGER NOT NULL,
    is_required     INTEGER NOT NULL DEFAULT 1 CHECK (is_required IN (0,1)),
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (checklist_id) REFERENCES checklists(id) ON DELETE CASCADE
);
CREATE INDEX ix_checklist_tasks_checklist ON checklist_tasks(checklist_id);
CREATE INDEX ix_checklist_tasks_active    ON checklist_tasks(is_active);

CREATE TABLE checklist_task_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id         INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    title           TEXT    NOT NULL,
    description     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (task_id) REFERENCES checklist_tasks(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_checklist_task_translation ON checklist_task_translations(task_id, language_code);

CREATE TABLE checklist_completions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id         INTEGER NOT NULL,
    user_id         INTEGER NOT NULL,
    checklist_id    INTEGER NOT NULL,
    shift_id        INTEGER,
    status          TEXT    NOT NULL CHECK (status IN ('COMPLETED','SKIPPED')),
    notes           TEXT,
    photo_url       TEXT,
    completed_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (task_id)      REFERENCES checklist_tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)      REFERENCES users(id)         ON DELETE SET NULL,
    FOREIGN KEY (checklist_id) REFERENCES checklists(id)    ON DELETE CASCADE,
    FOREIGN KEY (shift_id)     REFERENCES shifts(id)        ON DELETE SET NULL
);
CREATE INDEX ix_checklist_completions_task      ON checklist_completions(task_id);
CREATE INDEX ix_checklist_completions_user      ON checklist_completions(user_id);
CREATE INDEX ix_checklist_completions_checklist ON checklist_completions(checklist_id);
CREATE INDEX ix_checklist_completions_completed ON checklist_completions(completed_at);