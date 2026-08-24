-- V2.2 / V7: zones + zone_translations
--
-- A "zone" is a physical or logical area in the restaurant: e.g. Bếp phở,
-- Bún chả, Phục vụ, Kho. Each zone:
--   - has a unique code (used as the canonical id)
--   - has a rotating QR token (separate migration V7b adds qr_secret_hash)
--   - supports vi + ko translations (extensible to more languages)
--   - has a status (ACTIVE / DISABLED); soft-disable via status, never delete
--   - has a display color for the manager dashboard (HEX like #RRGGBB)
--
-- Status semantics:
--   - ACTIVE: zone is operational; staff can check in here
--   - DISABLED: zone is offline (renovation, hygiene issue); check-in rejected

CREATE TABLE zones (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    color           TEXT    NOT NULL DEFAULT '#3B82F6',
    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    required_staff  INTEGER NOT NULL DEFAULT 1 CHECK (required_staff >= 0),
    qr_token        TEXT    UNIQUE,
    qr_generated_at TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
CREATE INDEX ix_zones_status ON zones(status);
CREATE INDEX ix_zones_sort   ON zones(sort_order);

CREATE TABLE zone_translations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    zone_id         INTEGER NOT NULL,
    language_code   TEXT    NOT NULL CHECK (language_code IN ('vi','ko')),
    name            TEXT    NOT NULL,
    description     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
    FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_zone_translation ON zone_translations(zone_id, language_code);

-- ---------------------------------------------------------------------------
-- Seed: 4 default zones (configurable from Admin Dashboard in V2.5)
-- ---------------------------------------------------------------------------
INSERT OR IGNORE INTO zones (id, code, color, status, sort_order, required_staff) VALUES
    (1, 'BEP_PHO',   '#EF4444', 'ACTIVE', 1, 2),
    (2, 'BUN_CHA',   '#F59E0B', 'ACTIVE', 2, 2),
    (3, 'PHUC_VU',   '#10B981', 'ACTIVE', 3, 3),
    (4, 'KHO',       '#6366F1', 'ACTIVE', 4, 1);

INSERT OR IGNORE INTO zone_translations (zone_id, language_code, name, description) VALUES
    (1, 'vi', 'Bếp phở',    'Khu vực chế biến phở và nước dùng'),
    (1, 'ko', '쌀국수 주방',  '쌀국수와 육수를 조리하는 구역'),
    (2, 'vi', 'Bún chả',    'Khu vực chế biến bún chả và đồ nướng'),
    (2, 'ko', '분짜',         '분짜와 구이 조리 구역'),
    (3, 'vi', 'Phục vụ',    'Khu vực phục vụ khách hàng'),
    (3, 'ko', '홀 서빙',     '고객 서비스 구역'),
    (4, 'vi', 'Kho',        'Khu vực lưu trữ nguyên liệu'),
    (4, 'ko', '창고',         '식재료 보관 구역');