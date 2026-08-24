-- PHASE 2: initial schema
-- v1 entities: users, categories + category_translations, foods + food_translations + food_images,
--               store_settings + store_translations, audit_logs
--
-- Rules enforced by SQL (also re-asserted via JPA in entity classes):
--   - All id columns are INTEGER (SQLite ROWID alias) and AUTOINCREMENT
--   - Timestamps stored as ISO-8601 TEXT (UTC)
--   - Foreign keys ON (set in JDBC URL: ?foreign_keys=on)
--   - WAL mode (set in JDBC URL: ?journal_mode=WAL)
--   - Translations keyed by (parent_id, language_code) with UNIQUE
--   - Business content supports vi + ko in v1; schema is open to en/ja/zh later
--
-- NOTE: PRAGMA foreign_keys = ON is intentionally NOT in this file. Flyway 10+
-- refuses migrations that mix transactional (DML) and non-transactional
-- statements; PRAGMA counts as non-transactional. The PRAGMA is applied at
-- connection level via the JDBC URL (?foreign_keys=on) and via a dedicated
-- post-migration script (see DataSourceConfig) that runs after Flyway.

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL,
    password_hash   TEXT    NOT NULL,
    full_name       TEXT    NOT NULL,
    role            TEXT    NOT NULL CHECK (role IN ('ADMIN','STAFF')),
    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    lang            TEXT    NOT NULL DEFAULT 'vi'  CHECK (lang IN ('vi','ko')),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE UNIQUE INDEX ux_users_username ON users(username);
CREATE INDEX ix_users_role ON users(role);
CREATE INDEX ix_users_status ON users(status);

-- ---------------------------------------------------------------------------
-- categories  +  category_translations
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','HIDDEN')),
    image_url       TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE INDEX ix_categories_status ON categories(status);
CREATE INDEX ix_categories_sort ON categories(sort_order);

CREATE TABLE category_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id     INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    name            TEXT    NOT NULL,
    description     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_category_translation ON category_translations(category_id, language_code);

-- ---------------------------------------------------------------------------
-- foods  +  food_translations  +  food_images
-- ---------------------------------------------------------------------------
CREATE TABLE foods (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id     INTEGER NOT NULL,
    price           REAL    NOT NULL DEFAULT 0,
    image_url       TEXT,
    status          TEXT    NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','SOLD_OUT','HIDDEN')),
    is_featured     INTEGER NOT NULL DEFAULT 0 CHECK (is_featured IN (0,1)),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);
CREATE INDEX ix_foods_category ON foods(category_id);
CREATE INDEX ix_foods_status   ON foods(status);
CREATE INDEX ix_foods_featured ON foods(is_featured);

CREATE TABLE food_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id         INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    name            TEXT    NOT NULL,
    description     TEXT,
    ingredients     TEXT,
    portion         TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_food_translation ON food_translations(food_id, language_code);

CREATE TABLE food_images (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id         INTEGER NOT NULL,
    image_url       TEXT    NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE
);
CREATE INDEX ix_food_images_food ON food_images(food_id);

-- ---------------------------------------------------------------------------
-- store_settings  +  store_translations  (single-row table; id always = 1)
-- ---------------------------------------------------------------------------
CREATE TABLE store_settings (
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    logo_url        TEXT,
    address         TEXT,
    phone           TEXT,
    opening_hours   TEXT,
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE store_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id        INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    store_name      TEXT    NOT NULL,
    description     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (store_id) REFERENCES store_settings(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_store_translation ON store_translations(store_id, language_code);

-- ---------------------------------------------------------------------------
-- audit_logs
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER,
    action          TEXT    NOT NULL,
    entity          TEXT,
    entity_id       TEXT,
    details         TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX ix_audit_user    ON audit_logs(user_id);
CREATE INDEX ix_audit_action  ON audit_logs(action);
CREATE INDEX ix_audit_created ON audit_logs(created_at);

-- ---------------------------------------------------------------------------
-- Initial single-row store_settings (split into V1.1 to avoid mixed
-- transactional + non-transactional statements in V1 under Flyway 10+).
-- ---------------------------------------------------------------------------