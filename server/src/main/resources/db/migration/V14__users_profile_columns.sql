-- V2.2 / V14: extend users with nullable employee profile columns
--
-- Pure additive change. All new columns are NULLABLE so V1 users continue
-- to work without data migration. V1's `users` table (just rebuilt in V6)
-- already has all the original columns.
--
-- New columns support V2.4 (Employee Management):
--   phone              VARCHAR(20)
--   address            TEXT
--   avatar_url         TEXT
--   emergency_contact  VARCHAR(100)
--   joined_at          TEXT (ISO timestamp; defaults to created_at)
--   employment_status  TEXT CHECK IN ('ACTIVE','ON_LEAVE','TERMINATED','SUSPENDED')
--                      default 'ACTIVE' for new rows; existing rows unaffected
--
-- Note: `status` already exists with values ACTIVE / DISABLED. We
-- intentionally keep the original `status` as the master "login enabled"
-- flag and add `employment_status` for richer employee lifecycle states.
-- Disabling a user (status=DISABLED) does not change employment_status.
--
-- SQLite ALTER TABLE ADD COLUMN is fully supported and does not rewrite
-- the table. Each ALTER statement is non-transactional on its own but
-- since Flyway uses `mixed=true` (DDL + DDL are both non-transactional)
-- there is no mixed-statement error.

ALTER TABLE users ADD COLUMN phone             TEXT;
ALTER TABLE users ADD COLUMN address           TEXT;
ALTER TABLE users ADD COLUMN avatar_url        TEXT;
ALTER TABLE users ADD COLUMN emergency_contact TEXT;
ALTER TABLE users ADD COLUMN joined_at         TEXT;
ALTER TABLE users ADD COLUMN employment_status TEXT
    DEFAULT 'ACTIVE'
    CHECK (employment_status IN ('ACTIVE','ON_LEAVE','TERMINATED','SUSPENDED'));

CREATE INDEX ix_users_employment_status ON users(employment_status);