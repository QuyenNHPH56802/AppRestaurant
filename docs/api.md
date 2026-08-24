# API

> PHASE 3 complete. PHASE 4 will add menu endpoints. PHASE 7-8 will add admin/server endpoints.

## Envelope

All responses use the standard envelope:

```json
{ "success": true, "data": { }, "error": null, "meta": { "lang": "vi" } }
```

Errors:

```json
{ "success": false, "data": null, "error": { "code": "INVALID_CREDENTIALS", "message": "..." }, "meta": { "lang": "vi" } }
```

## Status codes

| Code | Meaning |
|---|---|
| 200 | OK |
| 400 | Validation error (`VALIDATION_FAILED` + per-field messages) |
| 401 | Unauthenticated |
| 403 | Forbidden (role check failed / disabled user) |
| 404 | Not found |
| 409 | Conflict |
| 429 | Rate limited |
| 500 | Internal server error |

## Language resolution

For endpoints that return localized business content:

1. `?lang=` query parameter (`vi` or `ko`)
2. `Accept-Language` HTTP header
3. Default: `vi`

The resolved language is also reported in `meta.lang` of every response.

## Auth (PHASE 3)

### POST /api/auth/login
Request:
```json
{ "username": "admin", "password": "admin123" }
```
Response 200:
```json
{ "success": true, "data": {
  "token": "<jwt>",
  "expiresInSeconds": 43200,
  "user": { "id": 1, "username": "admin", "fullName": "Quản trị viên", "role": "ADMIN", "lang": "vi" }
}, "error": null, "meta": { "lang": "vi" } }
```
Errors: 401 `UNAUTHORIZED` (bad credentials), 403 `FORBIDDEN` (disabled), 429 `RATE_LIMITED` (5/15min per username+IP).

### POST /api/auth/logout
Header: `Authorization: Bearer <jwt>`. Returns 200 with `data.message`. Stateless: client deletes the token.

### GET /api/me
Header: `Authorization: Bearer <jwt>`. Returns the current `UserSummary`.

## Health / server

### GET /api/health
Public. Returns `{ status, version, timestamp }`.

### GET /api/server/info
Public. Returns the LAN IP + port + protocol + version. PHASE 8 will turn this into the QR payload.

## Implemented

- PHASE 1: `GET /api/health`
- PHASE 2: error envelope, validation handler
- PHASE 3: `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/me`, `GET /api/server/info`
- PHASE 4: `GET /api/categories`, `GET /api/categories/{id}`, `GET /api/foods` (paged, q + categoryId + status + featured filters), `GET /api/foods/{id}`, `GET /api/foods/featured`, `GET /api/store`

## Admin (PHASE 7)

All `/api/admin/*` endpoints require a JWT with role `ADMIN`.

### Categories
- `GET /api/admin/categories?page&size` -> paged list (includes HIDDEN)
- `POST /api/admin/categories` body: `{ sortOrder, status, imageUrl, translations: [{lang,name,description}] }`
- `PUT /api/admin/categories/{id}` (full replace of translations)
- `PATCH /api/admin/categories/{id}` (status / sortOrder / imageUrl only)
- `DELETE /api/admin/categories/{id}` (soft-hide: sets status=HIDDEN)

### Foods
- `GET /api/admin/foods?page&size` -> paged list (includes HIDDEN)
- `POST /api/admin/foods` body: `{ categoryId, price, imageUrl, status, featured, sortOrder, translations: [{lang,name,description,ingredients,portion}] }`
- `PUT /api/admin/foods/{id}` (full replace of translations)
- `PATCH /api/admin/foods/{id}` (single fields)
- `PATCH /api/admin/foods/{id}/status` body `{ status }`
- `PATCH /api/admin/foods/{id}/featured` body `{ isFeatured }`
- `DELETE /api/admin/foods/{id}` (soft-hide)

### Users
- `GET /api/admin/users?page&size`
- `POST /api/admin/users` body: `{ username, password, fullName, role, status, lang }`
- `PUT /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}`
- `POST /api/admin/users/{id}/reset-password` body `{ newPassword }`

### Store
- `PUT /api/admin/store` body: `{ logoUrl, address, phone, openingHours, translations: [{lang,storeName,description}] }`

### Uploads
- `POST /api/admin/uploads/food-image` (multipart/form-data; field name = `file`). Allowed types: JPEG/PNG/WebP, max 5MB. Returns `{ imageUrl, size, contentType }`.

### Web admin SPA
- `GET /admin/app/` -> admin SPA (login + dashboard)
- `GET /admin/app/app.js`, `/admin/app/app.css` -> assets

## Coming in later phases

- PHASE 8: `/api/admin/backup`, `/api/admin/logs/stream`, `/api/admin/settings`, `/api/server/info` (extended)

## V2.3 / V18 — Notifications + Device Tokens

> Implements FCM-backed push notifications and the in-app feed so managers
> can push shift assignments / zone changes / messages to staff.

### Device tokens

#### POST /api/me/device-tokens

Register (or refresh) the caller's FCM token. Idempotent on (userId, token).

Headers: `Authorization: Bearer <jwt>`

```json
{ "token": "fcm-token-abc...", "platform": "ANDROID", "deviceId": "android-id-xxx", "appVersion": "1.0.0" }
```

Response:

```json
{ "success": true, "data": { "registered": true, "active": true, "activeDeviceCount": 2, "platform": "ANDROID" }, "error": null, "meta": { "lang": "vi", "fallback": [] } }
```

Validation: `token` 1..4096, `platform` ∈ {ANDROID, IOS, WEB}, `deviceId` ≤ 256,
`appVersion` ≤ 64.

#### DELETE /api/me/device-tokens

Deactivate one token (logout, app reinstall, or stale token cleanup).

```json
{ "token": "fcm-token-abc..." }
```

#### GET /api/me/device-tokens/count

Active device count for the caller.

### Notifications (in-app feed)

#### GET /api/me/notifications

Paged feed, newest first. `lang` is the locale of the rendered title/body.

Query: `?page=0&size=20&lang=vi`

Response (`data`):

```json
{ "items": [
    { "id": 1, "type": "SHIFT_ASSIGNED", "title": "...", "body": "...", "payloadJson": "{...}", "readAt": null, "createdAt": "2026-08-24T01:00:00Z" }
  ], "page": 0, "size": 20, "total": 1, "totalPages": 1 }
```

#### GET /api/me/notifications/unread-count

```json
{ "data": { "count": 3 } }
```

#### POST /api/me/notifications/{id}/read

Mark a single notification as read. Returns 404 if the notification
belongs to a different user (does not disclose existence).

#### POST /api/me/notifications/read-all

Bulk mark every unread notification for the caller as read. Returns
`{ "markedRead": <n> }`.

#### POST /api/me/notifications/{id}/respond

User accepts / declines a notification (typically a SHIFT_ASSIGNED push).

Body:

```json
{ "verdict": "ACCEPTED" } // or "DECLINED"
```

The verdict is encoded into the notification's existing `payloadJson` under
the `response` + `respondedAt` keys (no schema migration needed). Idempotent:
re-calling with the same verdict just refreshes the respondedAt timestamp.

#### GET /api/me/notifications/{id}/events

Audit trail — every push attempt for one notification. For debugging; each
event row carries the provider (FCM, Noop), status (SENT / FAILED / SKIPPED),
error code, and message id.

## V2.2 — Shifts, Zones, Checklists, Check-ins, Activity Log

> Restores the V2.2 staff-management endpoints that the V2.3 push pipeline
> now relies on. Every staff mutation that could affect another device (a
> shift assignment, a zone transfer, a checklist completion) fires a
> notification through `NotificationService.createAndDispatch` so the
> recipient sees it in their in-app feed + on FCM push if they have a token.

### Shifts (admin)

- `GET    /api/admin/shifts` — list shifts (includes inactive)
- `POST   /api/admin/shifts` body `{ name, description, startTime, endTime, tz, active, sortOrder }` (startTime/endTime = `HH:MM`)
- `PUT    /api/admin/shifts/{id}` (full update)
- `DELETE /api/admin/shifts/{id}` (soft-disable; preserves history)

### Shift assignments (admin)

- `GET    /api/admin/shift-assignments?date=YYYY-MM-DD` — daily view
- `POST   /api/admin/shift-assignments` body `{ shiftId, userId, date, status?, notes? }` — also fires `SHIFT_ASSIGNED` push
- `PUT    /api/admin/shift-assignments/{id}` (update status / notes)
- `DELETE /api/admin/shift-assignments/{id}` (history row deleted; soft-disable preferred for production)

### Shift assignments (me)

- `GET    /api/me/shifts` — the signed-in user's own assignment history (newest first)
- `POST   /api/me/shifts/{id}/respond` body `{ status, notes? }` where status ∈ `ACCEPTED | REJECTED | CHANGE_REQUESTED | CANCELLED`. Legal transitions are enforced: SCHEDULED/CONFIRMED → any of the above. Repeated calls against a terminal state (`ACCEPTED`, `REJECTED`, `COMPLETED`, `CANCELLED`, `SWAPPED`) return 409 `INVALID_TRANSITION`.

### Zones (admin)

- `GET    /api/admin/zones` (includes disabled rows)
- `POST   /api/admin/zones` body `{ code, color, status, sortOrder, requiredStaff, translations: [{lang,name,description}] }`
- `PUT    /api/admin/zones/{id}` (full replace; we explicitly delete child translations then re-insert to avoid the SQLite cascade + unique-key race observed during testing)
- `DELETE /api/admin/zones/{id}` (soft-disable)
- `GET    /api/admin/zones/{id}/current` — list users currently assigned to a zone

### Zone assignments

- `POST   /api/me/zones/assign` body `{ userId, zoneId, reason? }` — staff self-service; user can only move themselves
- `POST   /api/admin/zone-assignments` body `{ userId, zoneId, reason? }` — admin override; fires `ZONE_CHANGED` push to the affected user
- `GET    /api/me/zones/current` — caller's current zone
- `GET    /api/me/zones/history` — caller's zone history
- `GET    /api/admin/zone-assignments/user/{userId}` — admin view of a user's history

### Checklists (admin)

- `GET    /api/admin/checklists?zoneId=` — list (optionally filtered)
- `POST   /api/admin/checklists` body `{ zoneId, active, sortOrder, translations, tasks: [{ required, active, sortOrder, translations }] }`
- `PUT    /api/admin/checklists/{id}`
- `DELETE /api/admin/checklists/{id}` (soft-disable)

### Checklists (me)

- `GET    /api/me/checklists?zoneId=` — list active checklists
- `POST   /api/me/checklists/complete` body `{ taskId, status, notes?, photoUrl?, shiftId? }` where status ∈ `COMPLETED | SKIPPED`. Required tasks cannot be skipped (409).
- `GET    /api/me/checklists/completions?limit=` — recent completions

### Check-ins (me)

- `POST   /api/me/check-ins` body `{ zoneId, action, notes?, deviceId? }` where action ∈ `CHECK_IN | CHECK_OUT`. The server enforces "exactly one open check-in per user": a second `CHECK_IN` is rejected with 409 `ALREADY_CHECKED_IN`, and `CHECK_OUT` without a prior `CHECK_IN` returns 409 `NO_CHECK_IN`.
- `GET    /api/me/check-ins?limit=` — recent events for the caller

### Activity log (admin)

- `GET /api/admin/activity-logs?limit=&action=&entity=` — paginated business event log (login / shift assignment / zone transfer / check-in / checklist completion). Excludes `audit_logs` (security-sensitive); both run in parallel.

### Check-ins (admin) — Phase G

- `GET /api/admin/check-ins?page=&size=&userId=&zoneId=&action=` — paginated global feed; filterable by user, zone and action (`CHECK_IN` / `CHECK_OUT`). Caps `size` at 200. Returns `{ items, page, size, total }`.

### Notifications (admin) — Phase G / V2.3

- `GET /api/admin/notifications?page=&size=&type=&userId=` — paginated global feed. Each row carries the resolved `username`. Filterable by notification `type` and target `userId`.
- `GET /api/admin/notifications/{id}/events` — full delivery audit trail for one notification (one row per attempt, including retries).

### Device tokens (admin) — Phase G / V2.3

- `GET /api/admin/device-tokens?userId=&active=` — list every registered device token. **Sanitised**: never returns the raw token, only a 6-char `tokenPreview`, `tokenLength`, plus `platform`, `deviceId`, `appVersion`, `lastSeenAt`, `isActive`. Sortable by `lastSeenAt` descending.
- `GET /api/admin/device-tokens/stats` — per-user active device count (top-6 dashboard tiles).