# Session Tổng Hợp — Restaurant LAN System

> Ngày tạo: **2026-08-24** • Phiên làm việc: Phase C/D/E/G → **Phase F** (Android Notification UI) → Docs update.
>
> Tài liệu này tóm tắt **tất cả** những gì đã làm trong phiên này, **trạng thái hiện tại** của từng phần, **việc cần làm tiếp theo** để đến được bản demo cho khách hàng, và checklist **end-to-end smoke test** trước khi giao hàng.

---

## 1. Phases đã hoàn thành trong phiên này

| Phase | Phạm vi | Trạng thái | Tài liệu tham chiếu |
|-------|---------|-----------|---------------------|
| **C** | Server-side push notifications (FCM integration, idempotency, audit) | ✅ Done | `docs/architecture.md` § "Phase C" |
| **D** | Android in-app notification UI (deep link, accept/decline, badge polling) | ✅ Done | `docs/architecture.md` § "Phase D" |
| **E** | Bilingual translations for notifications (vi ↔ ko) | ✅ Done | `docs/api.md`, `i18n` files |
| **G** | Admin Dashboard UI (7 new views: Shifts / Zones / Checklists / Check-in log / Activity log / Notifications / Devices) | ✅ Done | `docs/architecture.md` § "Phase G" |
| **F** | **Android Notification UI enhancements** (pull-to-refresh, filter chips, pagination, events audit dialog, launcher shortcut, snackbar errors) | ✅ Done | `docs/architecture.md` § "Phase F", `docs/restaurant-user-guide.md` |

### Phase F — chi tiết deliverables

#### Backend / Architecture (no change)
- Server `MeNotificationController` đã expose đủ endpoints cần thiết (`/api/me/notifications`, `/api/me/notifications/unread-count`, `/api/me/notifications/{id}/read`, `/api/me/notifications/read-all`, `/api/me/notifications/{id}/events`, `/api/me/notifications/{id}/respond`).

#### Android — mới / sửa
- `notifications/NotificationsDataSource.kt` (mới) — interface thuần cho testability.
- `notifications/NotificationsDataSourceImpl.kt` (mới) — Hilt adapter.
- `notifications/NotificationsRepository.kt` — đổi thành `open class` (back-compat với `NotificationActionReceiver`, `UnreadBadgeHolder`).
- `notifications/NotificationsViewModel.kt` — refactor: dùng `NotificationsDataSource`; thêm state `refreshing`, `loadingMore`, `endReached`, `page`, `filter`, `events`, `loadingEvents`, `eventsForNotificationId`; thêm methods `pullToRefresh()`, `loadMore()`, `setFilter()`, `openEvents()`, `closeEvents()`, `clearError()`.
- `ui/notifications/NotificationsScreen.kt` (rewrite) — Filter chips (ALL / UNREAD_ONLY), pull-to-refresh overlay, pagination footer, empty state với retry, snackbar host, events dialog (`AlertDialog`), history icon trên mỗi row.
- `AppModule.kt` — wire `provideNotificationsDataSource(repo)`.
- `res/xml/shortcuts.xml` (mới) + `res/drawable/ic_notification_shortcut.xml` (mới) — static launcher shortcut "Mở thông báo".
- `AndroidManifest.xml` — `<meta-data android:name="android.app.shortcuts">` reference trên `MainActivity`.

#### i18n (vi + ko)
- 9 keys mới trong `values/strings.xml` + `values-ko/strings.xml`:
  - `common_close`
  - `notifications_filter_all`, `notifications_filter_unread`
  - `notifications_load_more`, `notifications_empty_unread`
  - `notifications_events`, `notifications_events_title`, `notifications_events_empty`
  - `notifications_shortcut_short`, `notifications_shortcut_long`

#### Tests
- `NotificationsViewModelTest.kt` (mới) — **12 JVM unit tests**:
  - refreshPopulatesItemsAndCount
  - unreadFilterHidesReadItems
  - markReadSetsTimestampAndDecrementsCount
  - markReadHidesItemUnderUnreadFilter
  - markAllReadHidesAllUnderUnreadFilter
  - respondUpdatesResponsesMapAndClearsResponding
  - respondRejectsInvalidVerdict
  - loadMoreAppendsItemsAndRespectsEndReached
  - refreshFailureStampsError
  - openEventsSurfacesAuditTrail
  - closeEventsClearsDialogState
  - clearErrorResetsErrorStamp
- Fake `NotificationsDataSource` hand-rolled — không cần mockito/robolectric.

#### Docs
- `docs/architecture.md` — section "Phase F — Android Notification UI (delivered)".
- `docs/manual-test-checklist.md` — checklist Phase F cho thiết bị thật.
- `docs/restaurant-user-guide.md` — hướng dẫn dùng cho **end-user** về màn hình Thông báo + section cho Admin về Notifications/Devices + troubleshooting mới.

### Server compile verification
- `mvn test -DskipTests` chạy pass trong `server/` — Phase F không động vào server code nên không cần test riêng.

---

## 2. Trạng thái hiện tại (end-of-session)

### ✅ Sẵn sàng demo (no blockers)
- Server boot + cài đặt Windows (Phase 1) — đã có sẵn `RestaurantServerSetup.exe` workflow.
- Admin Dashboard end-to-end: login → categories → foods → users → store → shifts → zones → checklists → check-ins → activity log → notifications → devices.
- Android Staff app end-to-end: pairing (QR/manual) → login → home → menu → food detail → profile → settings → shifts → zones → checklists → check-in → **notifications (Phase F)**.

### 🟡 Cần action trước khi demo
| # | Việc | Mức ưu tiên | Effort |
|---|------|-------------|--------|
| 1 | Build APK release từ Android source (`./gradlew :app:assembleRelease`) và bundle installer cho Server (`mvn package -DskipTests`) | **P0** | 1-2 giờ |
| 2 | Cấu hình FCM project thật (GoogleService-Info.plist + `restaurant.fcm.*` keys trong `server.properties`) | **P0** | 30 phút |
| 3 | Tạo data seed cho khách hàng demo: 2 danh mục, 8-10 món có ảnh thật, 4-5 nhân viên mẫu, 2 khu vực, 2 ca | **P1** | 1 giờ |
| 4 | Test trên thiết bị thật theo `docs/manual-test-checklist.md` (Phase C/D/F) — đặc biệt là launcher shortcut + history icon | **P1** | 1 giờ |
| 5 | Chuẩn bị laptop demo + máy in QR code (dán tường) cho khách hàng quét | **P1** | 30 phút |

### 🔴 Không nằm trong phiên này (chưa làm — còn open)
| # | Việc | Ghi chú |
|---|------|---------|
| A | E2E automated test pipeline (CI chạy `mvn test` + `./gradlew test` tự động) | Chưa có GitHub Actions; chạy tay mỗi lần. |
| B | Kiosk mode provisioning script (`adb shell dpm set-device-owner ...`) | Phase 12 đã có code; chưa có installer tự động cho khách hàng cuối. |
| C | Production HTTPS (TLS) + reverse proxy | Hiện bind `0.0.0.0:8080` HTTP thuần trên LAN — OK cho LAN nhà hàng nhưng không an toàn nếu mở ra ngoài. |
| D | Internationalization thêm (en, ja, zh) | Chỉ vi + ko hiện tại. |
| E | Backup-to-cloud (Google Drive / S3) | Chỉ backup local trong `%USERPROFILE%\RestaurantServer\backups\`. |
| F | Crash reporting (Firebase Crashlytics / Sentry) | Hiện không có — debug bằng `adb logcat`. |

---

## 3. Việc cần làm tiếp theo — End-to-End đến demo khách hàng

> Thứ tự ưu tiên: làm từ trên xuống. Mỗi bước có **acceptance criteria** rõ ràng.

### Bước 1 — Build production artifacts (P0)

**Server:**
```bash
cd server
mvn clean package -DskipTests
# Output: server/target/restaurant-server-1.0.0.jar + BOOT-INF/lib/*
```

**Android APK:**
```bash
cd android
./gradlew :app:assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk
```

**Acceptance:**
- File `.jar` start thành công, dashboard hiển thị ở `http://localhost:8080/admin/app/`.
- APK install trên Android 8+ thành công, app mở được màn hình pairing.

### Bước 2 — Smoke test trên máy demo (P0)

Làm theo `docs/manual-test-checklist.md` **end-to-end**:

1. ✅ Server install + start
2. ✅ LAN detection
3. ✅ Default credentials login
4. ✅ Android app install + pairing (QR)
5. ✅ Login + Home + Menu
6. ✅ Admin: tạo 2 categories, 5 foods, 1 user STAFF
7. ✅ Admin: tạo 2 shifts, assign cho STAFF
8. ✅ Admin: tạo 1 zone, 1 checklist
9. ✅ Staff: nhận push SHIFT_ASSIGNED (nếu FCM đã config)
10. ✅ Staff: **Phase F** — mở tab Thông báo, thấy feed, filter UNREAD_ONLY, tap history icon xem events dialog
11. ✅ Long-press app icon → "Mở thông báo" shortcut
12. ✅ Translations: đổi device language sang Korean, kiểm tra tất cả label Việt ↔ Hàn

### Bước 3 — Build Windows installer (P0)

Đóng gói `restaurant-server-1.0.0.jar` thành `RestaurantServerSetup.exe` bằng Inno Setup / WiX (script đã có sẵn trong `installer/` nếu có).

**Acceptance:**
- Double-click `.exe` trên Windows 10/11 máy sạch → cài đặt thành công → shortcut Desktop xuất hiện → click shortcut mở dashboard.
- Firewall rule **Restaurant Server 8080 (Private)** được tạo tự động.

### Bước 4 — Chuẩn bị môi trường demo cho khách (P1)

- Laptop Windows sạch (cài sẵn JDK 21 nếu cần — installer đã gồm JRE).
- Điện thoại Android demo (1-2 cái) đã cài APK.
- Wi-Fi hotspot riêng (không dùng Wi-Fi khách hàng để tránh xung đột IP).
- QR code in sẵn, dán lên tường hoặc để trên bàn.
- Tài khoản demo in sẵn (1 admin + 2 staff), mật khẩu đơn giản.

### Bước 5 — Pitch deck cho khách (P1)

Slide ngắn (5-7 trang) trình bày:

1. **Bài toán khách hàng** — quản lý ca, khu vực, thông báo nhân viên trong nhà hàng.
2. **Demo flow** — 7 bước (server start → pairing → login → quản lý món → phân ca → nhân viên nhận push → mở màn hình Thông báo + filter + history).
3. **Tính năng nổi bật** — offline-first LAN, không cần Internet (trừ push), backup tự động, badge real-time, audit trail.
4. **Bảo mật** — JWT 24h, bcrypt, role-based, firewall Private-only.
5. **Roadmap tiếp** — HTTPS, cloud backup, Kiosk mode auto-provisioning, thêm ngôn ngữ.
6. **Giá / Gói dịch vụ** — TODO (chưa có).
7. **Q&A**.

### Bước 6 — Sau demo (P2)

- Thu thập feedback → tạo issues.
- Ưu tiên fix blocker bugs trong 1 tuần.
- Ký hợp đồng pilot → onboard nhà hàng đầu tiên.

---

## 4. Smoke test script (30 phút) — chạy trước mỗi buổi demo

```bash
# 1. Server health
curl http://localhost:8080/actuator/health   # → {"status":"UP"}

# 2. Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# → save JWT

# 3. Verify notifications endpoints
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8080/api/me/notifications/unread-count
# → {"success":true,"data":{"count":0}}

# 4. Admin: list device tokens
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8080/api/admin/device-tokens
# → {"success":true,"data":{"items":[], ...}}
#   ↑ đảm bảo KHÔNG leak raw token (chỉ có tokenPreview 6-char)
```

**Trên Android device:**
1. Mở app → tab **Thông báo** ở dưới cùng → thấy danh sách rỗng + "Bạn chưa có thông báo nào".
2. Tap **Thử lại** → vẫn rỗng (không crash).
3. Long-press app icon trên launcher → xuất hiện "Mở thông báo".
4. Tap shortcut → mở thẳng tab Thông báo (không cần vào Home trước).
5. Đổi device language sang 한국어 → reload app → tất cả label mới phải bằng tiếng Hàn.

---

## 5. Known issues / tech debt

| # | Vấn đề | Ảnh hưởng | Workaround |
|---|--------|-----------|------------|
| 1 | Không có HTTPS | OK cho LAN, không cho public Wi-Fi | Demo chỉ trong LAN nội bộ nhà hàng |
| 2 | Không có CI/CD pipeline | Manual test mỗi lần build | Đã có `mvn test` + `NotificationsViewModelTest` chạy được local |
| 3 | Backup local only | Mất máy = mất data | Yêu cầu khách copy `backups/` ra USB mỗi tuần |
| 4 | FCM yêu cầu Internet | Push không hoạt động offline | In-app notification feed vẫn hoạt động (poll từ server) |
| 5 | Admin SPA là vanilla JS (không có framework) | Khó scale UI phức tạp | Phase G hiện tại đủ cho 7 views; nếu cần >15 views, refactor sang React/Vue |

---

## 6. Git — trạng thái & push plan

- Repo **chưa được init** tại `c:\AppRestaurant`. Sẽ:
  1. `git init`
  2. Tạo `.gitignore` (Android: `build/`, `*.iml`, `local.properties`; Server: `target/`, `*.log`)
  3. Add remote `https://github.com/QuyenNHPH56802/AppRestaurant.git` (đã cung cấp)
  4. Commit toàn bộ với message Phase F
  5. Push lên `main`

---

## 7. Next session — recommended opening

```
"continue from session-summary.md — finish step 1 (build artifacts), then step 2 (smoke test)"
```

→ Session sau sẽ: chạy `mvn package` + `./gradlew assembleRelease` → verify cả 2 build output → chạy smoke test trên máy local → fix blockers → commit → push.

---

**Người viết:** session ngày 2026-08-24 • Phiên Phase F • Cập nhật cuối cùng: trước khi push lên `QuyenNHPH56802/AppRestaurant`.