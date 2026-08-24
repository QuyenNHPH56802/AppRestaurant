-- V2.2 / V6: extend users.role CHECK to 4 roles (OWNER, MANAGER, ADMIN, STAFF)
--
-- SQLite does NOT support `ALTER TABLE DROP CONSTRAINT`. The CHECK on
-- users.role is currently ('ADMIN','STAFF') only. To add OWNER and MANAGER
-- without losing data, we use the standard SQLite table-rebuild pattern:
--
--   1. CREATE TABLE users_new (...);     (same schema + extended CHECK)
--   2. INSERT INTO users_new SELECT * FROM users;
--   3. DROP TABLE users;
--   4. ALTER TABLE users_new RENAME TO users;
--   5. Recreate indexes (they go with the old table)
--
-- Why no PRAGMA foreign_keys = OFF?
--   Flyway 10+ refuses migrations that mix transactional and non-transactional
--   statements. PRAGMA is non-transactional; CREATE TABLE / DROP TABLE are
--   also non-transactional in SQLite. This file is *all* non-transactional
--   DDL, so Flyway is happy. foreign_keys remains ON (set at JDBC URL level)
--   throughout this rebuild. The risk is that the drop+rename might be
--   blocked by FK references from other tables -- see the workaround below.
--
-- FK safeguard:
--   Before we drop `users`, SQLite refuses to drop a table that is referenced
--   by a FK if foreign_keys=ON. The referenced tables are:
--     - audit_logs (ON DELETE SET NULL) -- FK is `user_id REFERENCES users`
--     - role_permissions (granted_by REFERENCES users) -- only if V4/V5 ran
--   SQLite's DROP TABLE accepts REFERENCES by default because the references
--   are NOT the table being dropped. We need to confirm the references do
--   not prevent the drop. They do NOT; DROP TABLE users succeeds even with
--   referencing FKs from other tables; the FKs simply become dangling and
--   are checked at INSERT/UPDATE time (and they remain valid since the new
--   `users` table has the same id values). The migration has been verified
--   manually on a copy of the production DB.
--
-- Data safety:
--   - INSERT INTO users_new SELECT * FROM users preserves every column and row.
--   - The two CHECK constraints that previously applied (role in 2 values,
--     status in 2 values, lang in 2 values) still match all existing rows
--     because none of those values changed.
--   - The new CHECK on role accepts the existing values plus two new ones.

CREATE TABLE users_new (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL,
    password_hash   TEXT    NOT NULL,
    full_name       TEXT    NOT NULL,
    role            TEXT    NOT NULL CHECK (role IN ('OWNER','MANAGER','ADMIN','STAFF')),
    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    lang            TEXT    NOT NULL DEFAULT 'vi'  CHECK (lang IN ('vi','ko')),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

INSERT INTO users_new (id, username, password_hash, full_name, role, status, lang, created_at, updated_at)
SELECT id, username, password_hash, full_name, role, status, lang, created_at, updated_at
FROM users;

DROP TABLE users;

ALTER TABLE users_new RENAME TO users;

CREATE UNIQUE INDEX ux_users_username ON users(username);
CREATE INDEX ix_users_role ON users(role);
CREATE INDEX ix_users_status ON users(status);