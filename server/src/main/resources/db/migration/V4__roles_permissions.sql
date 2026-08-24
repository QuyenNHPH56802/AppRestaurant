-- V2.2 / V4: roles + permissions + role_permissions matrix
-- Purpose: introduce a permission-based authorization model alongside the
-- existing role enum on `users.role`. The `users.role` column itself is NOT
-- modified here (that happens in V6). This migration only adds the lookup
-- tables so permission codes and role names can be managed in the database
-- rather than as Java enums.
--
-- Conventions:
--   - All id columns are INTEGER PRIMARY KEY AUTOINCREMENT (SQLite ROWID alias)
--   - Timestamps stored as ISO-8601 TEXT (UTC)
--   - `code` columns are TEXT, unique, immutable (use for @PreAuthorize keys)
--   - `name` columns are localized JSON-free TEXT; translatable via app strings
--   - role_permissions is the source of truth for role->permission mapping
--   - Existing rows in `users` are NOT touched
--
-- Layering:
--   Phase 2.3 will introduce PermissionService that loads the matrix into a
--   per-request cache (loaded at login, embedded into the JWT claims).

CREATE TABLE roles (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE CHECK (code IN ('OWNER','MANAGER','ADMIN','STAFF')),
    description     TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE INDEX ix_roles_active ON roles(is_active);

CREATE TABLE permissions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    name_vi         TEXT    NOT NULL,
    name_ko         TEXT    NOT NULL,
    category        TEXT    NOT NULL,
    description     TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE INDEX ix_permissions_category ON permissions(category);

CREATE TABLE role_permissions (
    role_id         INTEGER NOT NULL,
    permission_id   INTEGER NOT NULL,
    granted_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    granted_by      INTEGER,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by)    REFERENCES users(id)       ON DELETE SET NULL
);
CREATE INDEX ix_role_permissions_role       ON role_permissions(role_id);
CREATE INDEX ix_role_permissions_permission ON role_permissions(permission_id);