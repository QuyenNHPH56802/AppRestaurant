# Architecture

> PHASE 1. This document is the high-level map. It will be extended in PHASE 2+ with concrete code references once features land.

## Goals

- One Windows server, many Android staff devices, on the same LAN.
- No cloud dependency.
- Customer receives two artifacts: `RestaurantServerSetup.exe` and `RestaurantStaff.apk`. No IDE, JDK, Node, or DB tooling on the customer's machine.
- Bilingual (vi + ko) with `vi` fallback. Server and Android UI both localize.
- Designed for Kiosk Mode rollout (Device Owner first, Screen Pinning fallback).
- Easy to extend in later phases (order, kitchen, payment, inventory, reports) without rewriting the foundation.

## Topology

```
[Windows PC]  RestaurantServer.exe (jpackage + JRE 21)
        |
        v
[Spring Boot 3]   http://<lan-ip>:8080
   |--- REST API (/api/...)
   |--- Static dashboard (/admin/...)
   |
   v
[SQLite]   data/restaurant.db  (WAL, FK=ON, app-internal)
   |
   v
[LAN]
   |
   +-- Android #1 (JWT) --+
   +-- Android #2 (JWT) --+-- Hilt + Retrofit + Compose
   +-- Android #3 (JWT) --+
```

The Android client never touches the database file. Everything is HTTP/REST with a Bearer JWT.

## Layer boundaries

### Server

| Layer | Responsibility | Tech |
|---|---|---|
| Controller | HTTP routing, DTO mapping, validation | Spring MVC |
| Service | Business rules, i18n, transaction boundaries | Plain Spring beans |
| Repository | Persistence abstraction | Spring Data JPA |
| Entity | Tables, relations | Hibernate (sqlite-jdbc dialect) |
| DTO | Wire format | POJOs |
| Security | JWT, BCrypt, role checks | Spring Security + custom filter |
| Bootstrap | First-run dir/db/admin/QR/IP | `ApplicationRunner` |
| Storage | File uploads + image validation | Local FS |
| Backup | SQLite online backup + retention | sqlite-jdbc backup API |
| Network | LAN IP detection, QR generation | `NetworkInterface` + ZXing |
| Log | Audit + request log | Logback + custom appender |

### Android

| Layer | Responsibility | Tech |
|---|---|---|
| ui (Compose) | Screens, theming, navigation | Material 3 + Navigation-Compose |
| viewmodel | StateFlow, intents | Lifecycle ViewModel + Hilt |
| repository | Cache + remote merge | Kotlin Coroutines + Flow |
| network | Retrofit, interceptors, DTO | Retrofit/OkHttp/Moshi |
| model | Domain entities | Plain Kotlin |
| storage | Server config + session | DataStore (Preferences) |
| auth | Token persistence | EncryptedSharedPreferences (or DataStore crypto) |
| kiosk | Lock task + Device Owner | `DevicePolicyManager` |
| i18n | Per-app locale | `AppCompatDelegate.setApplicationLocales` |

## Key invariants

1. **SQLite is app-internal.** No endpoint returns or accepts the file. No file server alias for `data/`.
2. **STAFF cannot read HIDDEN foods.** Enforced at the repository query, not the controller.
3. **All UI text is localized.** No Vietnamese/Korean literals in code; only `R.string.*` keys and server `messages_*.properties`.
4. **Translations fall back to vi.** When a translation is missing, the vi content is returned.
5. **JWT is the only auth scheme.** No session cookies. Logout invalidates by client-side delete (no Redis in v1).
6. **Server is LAN-only by default.** No Internet exposure; cleartext HTTP allowed only on the private subnet (`networkSecurityConfig`).
7. **WAL + FK ON + parameterized queries** for all writes.
8. **Rate-limited login** (5/15min per username+IP).
9. **Backup never overwrites the live db file**; uses SQLite backup API and writes to `/backups/`.
10. **Kiosk Mode is opt-in** and requires an admin PIN to exit; documented provisioning via `dpm set-device-owner` (Device Owner) or `startLockTask()` (Screen Pinning).

## Extensibility hooks (out of scope for v1, designed in)

- `Order`, `OrderItem`, `Table` — repositories not yet created, but the `categories`/`foods` schema leaves room.
- Role hierarchy in `SecurityConfig` prepared for `MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`, `BAR`, `WAREHOUSE`.
- Translation tables generalize: any new business content table (e.g. `promotions`, `combos`) can follow the `*_translations` pattern.
- Server dashboard exposes device count and login events, ready to back a future `audit_logs` view.

## Open items

- Brand assets (logo, restaurant name) — TBD with customer.
- HTTPS in LAN? Default is cleartext + private firewall rule. Decision needed if customer has compliance requirements.
- Default port 8080. Confirm no conflict.
- Android min SDK 24 / target SDK 34. Confirm.

## Phase 1 deliverables (this commit)

- Folder skeleton (server, android, installer, docs)
- Spring Boot bootable app exposing `GET /api/health` and a placeholder `/admin/` dashboard
- Android app that builds and runs a single Compose screen with vi+ko strings, Hilt graph stub
- `docs/architecture.md` (this file) + `README.md`

## V2.3 / V18 — Notifications + Device tokens (delivered)

### Server (V2.3)

- `entity.Notification` + `entity.NotificationEvent` + `entity.DeviceToken`
- `notify.NotificationProvider` abstraction with two impls:
  `NoopNotificationProvider` (default — used when FCM is not configured)
  and `FcmNotificationProvider` (lazy `FirebaseApp`, batched multicast,
  scrubs tokens that come back PERMANENT_FAILURE).
- `service.NotificationService` — `createAndDispatch` is idempotent on
  `idempotencyKey` so a retry of the same logical push never creates a
  duplicate row. Read side: `list`, `unreadCount`, `markRead`,
  `markAllRead`, plus the V18 additions `respond` and `readResponse`.
- `service.DeviceTokenService` — 180-day token cleanup cron.
- `controller.MeNotificationController` — 5 endpoints under `/api/me/notifications`.
- `controller.DeviceTokenController` — register / unregister / count.
- 56 server-side tests, all passing (V22 controllers add 7 more — total now 63).

### Android (V18)

- `fcm.RestaurantFirebaseApp` — manual FirebaseOptions wired from
  `BuildConfig.FCM_*` fields populated by `local.properties`. No
  google-services.json in the repo, no google-services Gradle plugin.
- `fcm.RestaurantFcmService` — `FirebaseMessagingService` that renders
  pushes into the correct Android channel
  (`fcm_default` / `fcm_shift` / `fcm_zone` / `fcm_manager`), using the
  current user's locale so vi users see Vietnamese titles and ko users see
  Korean titles.
- `fcm.TokenRotator` — only POSTs to `/api/me/device-tokens` when the
  token actually changed (extracted into `TokenRotationGuard` for unit
  testing). Called from login and from `onNewToken`.
- `fcm.NotificationActionReceiver` — BroadcastReceiver for Accept /
  Decline taps on the notification itself; survives the app being killed.
- `ui.notifications.NotificationsScreen` — in-app feed with mark-read,
  mark-all-read, and Accept / Decline buttons for respondable types.
- `ui.notifications.NotificationPermissionDialog` — runtime grant on
  Android 13+ via `ActivityResultContracts.RequestPermission`, one-shot
  via DataStore flag.
- `notifications.UnreadBadgeHolder` — process singleton, 30s polling,
  driven by `AuthRepository.start/stop`.
- 22 JVM unit tests (TokenRotationGuard 5 + NotificationChannels 5 +
  NotificationPayload 7 + 5 others from earlier phases), all passing.

## V2.2 — Operational endpoints (delivered)

### Server

- `entity.ShiftAssignment.Status` updated to the V17 8-state enum
  (`SCHEDULED`, `CONFIRMED`, `ACCEPTED`, `REJECTED`, `CHANGE_REQUESTED`,
  `COMPLETED`, `CANCELLED`, `SWAPPED`); status updates flow through JPA
  with the database CHECK constraint backing it.
- `service.V22Service` — single service for shifts, zones, checklists,
  check-ins and activity logs. Notifications are dispatched inline
  (`SHIFT_ASSIGNED`, `SHIFT_RESPONSE`, `ZONE_CHANGED`) so every meaningful
  staff mutation shows up in the recipient's feed without the admin having
  to remember to wire it.
- `controller.V22Controllers` — split into 8 `@RestController` inner
  classes (admin + me variants for shifts / zones / checklists / check-ins
  and one admin-only activity log controller). Pattern matches the V1
  admin controllers so the existing security config keeps working.
- Zone updates explicitly `clear()` and `flush()` the child translations
  before re-inserting. The JPA orphan-removal path was racing the unique
  index on `(zone_id, language_code)` under SQLite — the explicit flush
  fixes it.
- 7 new integration tests in `V22ApiTest` covering shift CRUD +
  staff-side accept/reject transitions, zone CRUD + self-service
  assignment, checklist create + complete (with the required-task
  skip-rejection path), check-in/check-out, and the activity log
  authorisation boundary.

### Android

- `network.V22Api` + `repository.V22Repository` — Retrofit wrapper
  around the 12 new endpoints; Moshi adapters generated by KSP.
- `ui.shifts`, `ui.zones`, `ui.checklists`, `ui.checkin` — four Compose
  screens reachable from the Settings screen (so the bottom-nav stays lean
  for the food browsing flow). Each one wraps a Hilt ViewModel that
  refreshes on init and surfaces errors inline.
- Shifts screen renders the four staff actions (Accept / Reject / Change /
  Cancel) for any row still in `SCHEDULED` or `CONFIRMED`.
- Zones screen shows the "current zone" highlight (sourced from
  `/api/me/zones/current`) and lets the user move themselves via the
  self-service endpoint; the server rejects other-user assignments.
- Checklists screen shows the checklist tree with per-task Complete +
  optional Skip buttons.
- Check-in screen lists every active zone with paired `In` / `Out` buttons
  + recent events panel.
- 6 new JVM unit tests in `V22DtoTest` exercising JSON round-trip for the
  4 most complex DTOs.

## Phase G — Admin Dashboard UI (delivered)

The admin SPA (`static/admin/app/`) now exposes six new views on top of
the existing Categories / Foods / Users / Store screens. They run entirely
client-side (vanilla JS, no framework) against the existing REST API.

### Server (Phase G)

- `controller.AdminNotificationController` — paginated global notification
  feed (`GET /api/admin/notifications`), per-notification event trail
  (`GET /api/admin/notifications/{id}/events`), and two device-token
  endpoints (`GET /api/admin/device-tokens`, `GET /api/admin/device-tokens/stats`).
  The token itself is **never** returned — only a 6-char preview + length,
  plus the platform/device/app metadata needed to diagnose install state.
- `controller.V22Controllers.AdminCheckInController` — admin browse of
  every `check_in_log` row, filterable by user/zone/action.
- `service.V22Service.toCheckInViews(...)` — shared resolver used by both
  the me-recent endpoint and the admin browse endpoint; batches lookups by
  user-id and zone-id for efficiency.
- `repository.CheckInLogRepository.findAllByOrderByCreatedAtDesc(Pageable)`
  and `repository.NotificationRepository.findAllByOrderByCreatedAtDesc(Pageable)`
  — global paginated feeds (used only by the admin views).

### Admin SPA (Phase G)

| View | Purpose | Key API |
|------|---------|---------|
| Shifts   | CRUD on shift templates + per-day assignment table with date picker | `/api/admin/shifts`, `/api/admin/shift-assignments?date=` |
| Zones    | CRUD on zones (with bilingual modal) + "current assignments" peek + reassign button | `/api/admin/zones`, `/api/admin/zones/{id}/current`, `/api/admin/zone-assignments` |
| Checklists | CRUD on checklists + tasks (bilingual), per-checklist task list | `/api/admin/checklists` |
| Check-in log | Filterable feed of all check-in events (by user/zone/action) | `/api/admin/check-ins` |
| Activity log | Filterable feed of all business activity entries | `/api/admin/activity-logs` |
| Notifications | Global feed of in-app notifications + per-row delivery events modal | `/api/admin/notifications`, `/api/admin/notifications/{id}/events` |
| Devices  | All device tokens (sanitised) + per-user active count tiles | `/api/admin/device-tokens`, `/api/admin/device-tokens/stats` |

### Tests (Phase G)

- `AdminSpaSmokeTest` — 3 integration tests confirming the admin shell,
  the JS bundle and the CSS bundle are still served and now expose the
  Phase G markers (`renderShifts`, `renderZones`, …). Future view additions
  should extend this test rather than re-invent smoke coverage.

## Phase F — Android Notification UI (delivered)

The Android notification screen was already wired (deep-link, accept/decline,
badge polling) during Phase D. Phase F turns it into a proper feed experience:
pagination, pull-to-refresh, ALL/UNREAD filtering, audit-trail dialog, snackbar
errors, and a launcher shortcut for one-tap access.

### Architecture

- `notifications.NotificationsDataSource` — pure interface extracted from
  `NotificationsRepository` so unit tests can swap in a hand-rolled fake
  without dragging in Robolectric. The original repository stays `open` for
  backwards compatibility with the few other callers
  (`NotificationActionReceiver`, `UnreadBadgeHolder`).
- `notifications.NotificationsDataSourceImpl` — Hilt-bound adapter that
  delegates the interface to the existing repository. Wired in `AppModule`.
- `notifications.NotificationsViewModel` — now depends on the interface
  (testable). New state fields:
    - `refreshing`, `loadingMore`, `endReached`, `page`, `filter`
    - `events`, `loadingEvents`, `eventsForNotificationId` for the audit dialog
    - `error` (non-blocking) + `clearError()`
- `ui/notifications/NotificationsScreen.kt` — refactored to render:
    - **Filter chips** (ALL / UNREAD_ONLY) at the top.
    - **Pull-to-refresh** via a `refreshing` overlay + `pullToRefresh()` VM call.
    - **Pagination footer** ("Tải thêm…" button + loading spinner).
    - **Empty state** with "Thử lại" retry, separately worded for the
      UNREAD_ONLY filter.
    - **Snackbar host** for transient errors (no more silent failures).
    - **Events dialog** (`AlertDialog`) that loads
      `GET /api/me/notifications/{id}/events` for one notification and
      surfaces the per-attempt delivery audit trail.
    - **History icon** on every row to open the events dialog.

### Launcher shortcut

- `res/xml/shortcuts.xml` declares a static shortcut that deep-links into
  the notifications screen (`restaurant://staff/notifications`).
- `AndroidManifest.xml` references it via `<meta-data>` on `MainActivity`.
  Long-pressing the launcher icon now shows a "Mở thông báo" entry — the
  launcher's standard way to surface a notification count without
  third-party badge libraries (which historically break on OEM launchers).

### Tests

- `NotificationsViewModelTest` — 12 JVM unit tests covering refresh, filter
  toggling, pagination append, mark-read / mark-all-read (including
  filter-aware item hiding), accept/decline verdict round-trip, invalid
  verdict rejection, error stamping + clearing, and events dialog open/close.
  Hand-rolled `FakeNotificationsDataSource` keeps the test classpath
  free of Robolectric / mockito-kotlin.