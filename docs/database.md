# Database

> PHASE 2 complete. The schema lives across 18 Flyway migrations under `server/src/main/resources/db/migration/` (V1 through V18). JPA entities are mirrored in `server/src/main/java/com/restaurant/server/entity/`.

## Engine

SQLite (xerial sqlite-jdbc). Single file: `<data-dir>/restaurant.db`.

The JDBC URL is configured in `application.yml` and enables:
- `journal_mode=WAL`
- `foreign_keys=on`
- `busy_timeout=5000`

Hibernate runs in `ddl-auto: validate` mode; Flyway owns the schema.

## Tables

### Core (V1–V3)
- `users`
- `categories`, `category_translations`
- `foods`, `food_translations`, `food_images`
- `store_settings` (single-row, `id = 1`), `store_translations`
- `audit_logs`

### Roles & permissions (V4–V6)
- `roles` — ADMIN, STAFF (and reserved: MANAGER, KITCHEN)
- `permissions` — fine-grained capability keys (e.g. `food.create`, `shift.assign`)
- `user_roles` — many-to-many between `users` and `roles`
- `role_permissions` — many-to-many between `roles` and `permissions`

### Zones (V7–V9)
- `zones` — restaurant zones (kitchen, bar, hall, …) with `color_hex` and `sort_order`
- `zone_translations` — vi / ko name + description per zone
- `zone_assignments` — current and historical zone assignments (one row per user-zone; `is_current` flag marks the active assignment)

### Operational (V8–V11)
- `check_in_logs` — staff check-in / check-out events per zone
- `checklists` — admin-defined checklists, optionally bound to a zone
- `checklist_translations` — vi / ko titles
- `checklist_tasks` — task rows within a checklist
- `checklist_task_translations` — vi / ko task names + descriptions
- `checklist_completions` — per-user completion records per task
- `shifts` — shift templates (start / end / role)
- `shift_assignments` — per-user assignment rows with full V17 status enum (`SCHEDULED`, `CONFIRMED`, `ACCEPTED`, `REJECTED`, `CHANGE_REQUESTED`, `COMPLETED`, `CANCELLED`, `SWAPPED`)

### User profile (V14)
- Adds `display_name`, `phone`, `avatar_url`, `locale`, `last_login_at` columns to `users`.

### Notifications & remote (V12, V15–V18)
- `notifications` — server-side notification log + idempotency key column (V12 + V18)
- `device_tokens` — FCM tokens registered per employee (multi-device)
- `notification_events` — append-only feed of notification side-effects (sent / failed / skipped-dedup) used by the audit trail and idempotency layer

All `id` columns are `INTEGER PRIMARY KEY AUTOINCREMENT` (SQLite ROWID). Timestamps are stored as ISO-8601 UTC strings and mapped to `Instant` in the JPA entities via `@PrePersist`/`@PreUpdate`.

## Multi-language

```
foods (id, category_id, price, image_url, status, is_featured, sort_order, created_at, updated_at)
food_translations (id, food_id, language_code, name, description, ingredients, portion)
```

Same pattern for `categories` and `store_settings`.

`language_code` is `vi` or `ko` in v1. The schema, JPA queries, and the server-side translator (built in PHASE 4) are open to additional languages without code changes.

## Seed (`V2__seed.sql`)

- 2 users (`admin`, `nhanvien01`) with placeholder password hashes that are overwritten by `bootstrap/AdminBootstrap` in PHASE 3 with proper BCrypt hashes.
- 5 categories with vi + ko translations.
- 25 foods with vi + ko translations, mix of `AVAILABLE` / `SOLD_OUT` / `HIDDEN`, several `is_featured = 1`.
- 2 store translations for the singleton `store_settings` row.

## Invariants enforced by repositories

- `FoodRepository.findAllVisible` excludes `HIDDEN` foods. STAFF can never see them.
- `FoodRepository.findFeatured` returns only `AVAILABLE && featured = true`.
- `FoodRepository.searchByLanguage` searches the translation table for the requested language, still excluding `HIDDEN`.
- Admin endpoints use `findAllAdmin` which returns everything (admin needs to see `HIDDEN` to manage).
- `ZoneRepository` enforces that deleting a `zone` with an active assignment is refused (admin must reassign first).
- `ShiftAssignmentRepository` enforces the V17 status state-machine in service code: once an assignment is `ACCEPTED`, `COMPLETED`, `CANCELLED`, `REJECTED`, or `SWAPPED`, the staff cannot transition it further (returns `409 INVALID_TRANSITION`).
- `NotificationRepository` is backed by a unique idempotency key per (recipient, event-type, entity-id) tuple. Duplicate sends return the existing notification row instead of creating a new one.

## Migration timeline

| Migration | Purpose |
|-----------|---------|
| V1 | Initial core schema (users, foods, categories, store, audit) |
| V2 | Seed (admin + staff users, foods, translations) |
| V3 | `store_settings` row |
| V4–V6 | Roles + permissions + seed |
| V7 | Zones + zone translations |
| V8 | Check-in logs |
| V9 | Zone assignments |
| V10 | Checklists + tasks + translations |
| V11 | Shifts |
| V12 | `notifications` table |
| V13 | `activity_logs` table |
| V14 | `users` profile columns |
| V15 | `device_tokens` (FCM) |
| V16 | `notification_events` (append-only feed) |
| V17 | `shift_assignments.status` enum extension (8 states) |
| V18 | `notifications` idempotency key column |

## Backup

- Manual: `POST /api/admin/backup` (PHASE 9)
- Scheduled daily at 02:00 server local time (configurable)
- Retention: 30 daily + 12 weekly
- Stored under `<backups-dir>/backup_<yyyy-MM-dd_HHmmss>.db`
- `PRAGMA integrity_check` after every backup
- Restore via admin dashboard file picker