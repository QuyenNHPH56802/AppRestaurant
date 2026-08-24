-- ---------------------------------------------------------------------------
-- Insert initial single-row store_settings.
-- Split out of V1 because Flyway 10+ rejects mixed transactional +
-- non-transactional statements in a single migration. V1 contains DDL
-- (CREATE TABLE / INDEX / PRAGMA), this file is purely DML.
--
-- Use INSERT OR IGNORE because production databases migrated under the
-- original V1 already have row id=1 (the old V1 issued this INSERT
-- itself); ignoring the duplicate keeps the migration idempotent.
-- ---------------------------------------------------------------------------
INSERT OR IGNORE INTO store_settings (id, logo_url, address, phone, opening_hours)
VALUES (1, NULL, NULL, NULL, NULL);