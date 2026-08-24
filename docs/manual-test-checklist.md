# Manual Test Checklist

> PHASE 10 manual verification checklist. Run on the customer's machine after PHASE 11 packaging is complete.

## Server install

- [ ] Run `RestaurantServerSetup.exe` as administrator
- [ ] Accept default install path
- [ ] Verify Windows Firewall prompt and "Allow on Private" answer
- [ ] Server starts, browser at `http://127.0.0.1:8080/admin/` shows the dashboard
- [ ] `http://127.0.0.1:8080/api/health` returns JSON
- [ ] `%USERPROFILE%\RestaurantServer\` contains `data/`, `uploads/`, `backups/`, `logs/`, `config/`

## LAN detection

- [ ] `http://<server-lan-ip>:8080/api/server/info` returns the correct LAN IP (not 127.0.0.1)
- [ ] Dashboard's QR code (`/api/server/qr.png`) shows server IP+port

## Default credentials

- [ ] `admin / admin123` logs in (after install only — should be changed on first production use)
- [ ] `nhanvien01 / staff123` logs in
- [ ] 6 failed logins within 15 minutes triggers 429

## Android app

- [ ] Install `RestaurantStaff.apk` on a phone on the same Wi-Fi
- [ ] Open the app — splash → Pairing screen
- [ ] Scan QR → app pairs, navigates to Login
- [ ] Manual entry works (e.g. `192.168.1.10:8080`)
- [ ] Login with admin → Home shows featured foods + popular
- [ ] Menu tab shows categories chips + search + items
- [ ] Tap a food → detail view shows vi + ko translations
- [ ] Switch language in Settings → vi ↔ ko switches the UI on the fly
- [ ] Profile tab shows username / role / language
- [ ] Logout returns to login screen

## Translations

- [ ] Korean names render correctly for foods that have ko translations
- [ ] When a row lacks the ko translation, the vi text is shown and the API returns `meta.fallback = ["vi"]`
- [ ] Changing language on Android triggers a fresh API call with `?lang=` parameter

## Admin dashboard

- [ ] Open `http://<server-lan-ip>:8080/admin/app/`
- [ ] Login with admin → Tổng quan shows total/available/sold_out/hidden/foods/categories stats
- [ ] Danh mục tab: add, edit, hide
- [ ] Món ăn tab: add, edit, upload image, change status (AVAILABLE/SOLD_OUT/HIDDEN), toggle featured
- [ ] Nhân viên tab: add user, reset password, disable
- [ ] Cửa hàng tab: edit store name vi + ko, address, phone, hours
- [ ] After edits, the Android app's `/api/foods?...&lang=...` returns the new content

## Server dashboard

- [ ] Open `http://<server-lan-ip>:8080/admin/server/`
- [ ] Status, version, uptime, data size, backup count all visible
- [ ] QR code image renders
- [ ] "Sao lưu ngay" creates a new backup file in the list
- [ ] Stop the server, restart, the new backup still exists
- [ ] Old backups beyond retention are removed automatically

## Backup / restore

- [ ] Create a food via admin, run manual backup, name has timestamp
- [ ] Drop that food, run restore (manual: stop server, copy backup file to `data/restaurant.db`)
- [ ] Food is back after restart

## Error handling

- [ ] Disconnect Wi-Fi, try to login → app shows network error, doesn't crash
- [ ] Reconnect Wi-Fi → app retries successfully
- [ ] Stop the server while the Android app is running → app keeps the cached list until next API call fails gracefully
- [ ] Start the server → next API call succeeds

## Security

- [ ] STAFF user cannot hit `/api/admin/*` (returns 403)
- [ ] Login with bad password returns 401
- [ ] Rate limit blocks 6+ failed logins with 429
- [ ] After 5+1 failed logins, even the correct password is locked out until window expires

## Kiosk Mode (PHASE 12 verification, run after PHASE 12)

- [ ] Enable Kiosk Mode in Settings, set admin PIN
- [ ] Home key / Back key are intercepted
- [ ] Status bar hidden (when supported)
- [ ] Re-opening the app prompts for admin PIN to exit

## Performance sanity

- [ ] Cold start of server < 5s
- [ ] Cold start of Android app to Login < 2s (depends on device)
- [ ] Food list page renders within 500ms over Wi-Fi

## Internationalization

- [ ] Switch vi → ko without restart of the app
- [ ] All visible strings change; no English fallbacks visible
- [ ] Switching language re-fetches food list with the new `?lang=`

## V2.3 / V18 — Push notifications (Phase C+D)

> Server endpoints land in PHASE V2.3, Android wiring in PHASE V18.

### Server-only (no Firebase project needed)

These work against any build of the server because FCM is noop when no
service-account JSON is configured. The in-app REST feed is the source of
truth for verification.

- [ ] `POST /api/me/device-tokens` with admin JWT → response includes `activeDeviceCount=1`
- [ ] Re-POST same token → idempotent, no new row
- [ ] `DELETE /api/me/device-tokens` → response `active=false`
- [ ] `GET /api/me/notifications/unread-count` → starts at 0 after fresh seed
- [ ] `POST /api/admin/notifications` (manager sends a SHIFT_ASSIGNED push via the admin SPA)
- [ ] `GET /api/me/notifications?lang=vi` → new row appears, `readAt=null`
- [ ] `POST /api/me/notifications/{id}/read` → next unread-count drops by 1
- [ ] `POST /api/me/notifications/read-all` → unread-count drops to 0
- [ ] `POST /api/me/notifications/{id}/respond` with `{"verdict":"ACCEPTED"}` → response `{verdict:"ACCEPTED"}`
- [ ] Re-fetch notification row → `payloadJson` contains `"response":"ACCEPTED"` and `respondedAt`
- [ ] Same notification in ko: `?lang=ko` → Korean title/body, server still echoes verdict
- [ ] `GET /api/me/notifications/{id}/events` → at least one row (status SKIPPED if FCM disabled)

### Android device (with a Firebase project configured)

If `local.properties` has `restaurant.fcm.*` keys filled in AND
`restaurant.fcm.enabled=true` in the server config:

- [ ] First launch prompts for POST_NOTIFICATIONS on Android 13+
- [ ] Tap "Cho phép" / "허용" → permission granted, no re-prompt
- [ ] Server sends a notification → phone wakes with the right channel + sound
- [ ] Tap notification → opens the app on the notification detail
- [ ] In-app Notifications tab shows a badge count matching `unread-count`
- [ ] SHIFT_ASSIGNED notification has Accept / Decline action buttons
- [ ] Tap Accept → server `respond` endpoint called, in-app row flips to "Đã nhận" / "수락됨"
- [ ] Token rotation (reinstall app) → the server's device-tokens table grows the active count then back

### Phase F — Notification UI

- [ ] In-app Notifications tab shows ALL / UNREAD_ONLY filter chips at the top
- [ ] Switch filter → list reloads, only matching items stay visible
- [ ] Mark one notification read while UNREAD_ONLY is active → row disappears, unread badge drops by 1
- [ ] "Mark all read" → unread badge → 0; UNREAD_ONLY list empties immediately
- [ ] Tap the history (clock) icon on any row → "Lịch sử gửi thông báo" dialog opens with per-attempt rows
- [ ] Pagination footer: with >20 notifications, "Tải thêm…" appends the next page until endReached
- [ ] Pull-to-refresh: header spinner appears, list reloads from page 0
- [ ] Empty state: with no notifications, the screen shows "Bạn chưa có thông báo nào" + "Thử lại" button
- [ ] Long-press the launcher icon → "Mở thông báo" shortcut launches directly into the Notifications screen
- [ ] i18n: switch device language to Korean → all new strings render in Korean (필터, 더 보기, 알림 전송 기록, 닫기)

### Performance sanity

- [ ] Cold start of server < 5s
- [ ] Cold start of Android app to Login < 2s (depends on device)
- [ ] Food list page renders within 500ms over Wi-Fi

## V2.2 operational flows

### Shifts (admin)

- [ ] Create shift `Ca sáng 06:00-14:00`; appears in admin list
- [ ] Assign shift to staff user; staff receives push + in-app notification (`SHIFT_ASSIGNED`)
- [ ] Staff taps Accept — server records `ACCEPTED`; admin sees the new status
- [ ] Staff taps Reject after Accept — server returns 409 `INVALID_TRANSITION`
- [ ] Admin deletes the assignment — server removes the row

### Zones

- [ ] Create zone `TEST_*` with vi + ko translations; appears in `GET /api/me/zones`
- [ ] Update zone color / sort order — translations survive the update without UNIQUE-constraint errors
- [ ] Staff self-service `/api/me/zones/assign` moves them to the new zone; `current` becomes true
- [ ] Staff tries to assign another user via the self-service endpoint — 403 `FORBIDDEN`

### Checklists

- [ ] Create checklist with 2 tasks (1 required, 1 optional)
- [ ] Staff completes required task — appears in `completions`
- [ ] Staff tries to SKIP a required task — 409 `CANNOT_SKIP`
- [ ] Staff SKIPs the optional task — recorded as `SKIPPED`

### Check-ins

- [ ] Staff CHECK_IN to zone A — recent feed shows it
- [ ] Second CHECK_IN without prior CHECK_OUT — 409 `ALREADY_CHECKED_IN`
- [ ] Staff CHECK_OUT — recent feed shows it
- [ ] CHECK_OUT without prior CHECK_IN — 409 `NO_CHECK_IN`

### Activity log

- [ ] `/api/admin/activity-logs` shows the events above
- [ ] Staff cannot reach the endpoint (403)

## Pass criteria

All checkboxes above must pass before delivery.