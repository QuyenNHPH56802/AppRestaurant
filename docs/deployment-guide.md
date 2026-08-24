# Deployment guide

> Covers server (Windows) and Android (LAN-first). Final updates land in PHASE 11 packaging.

## Server (Windows)

### Requirements
- Windows 10 64-bit or Windows 11
- Local administrator account (first run only, for Windows Firewall rule)
- LAN reachable IP (DHCP reservation recommended)
- **Internet access required for push notifications** (FCM). Without Internet, the server still works fully on LAN — push notifications will be queued and dispatched when connectivity returns.

### Install
1. Run `RestaurantServerSetup.exe`.
2. Accept the default install path: `C:\Program Files\Restaurant\RestaurantServer`.
3. The installer creates a Windows Firewall inbound rule on the **Private** profile only, port 8080.
4. Optionally create a desktop shortcut to the dashboard URL.

### First run
- Server creates `<USER_HOME>\RestaurantServer\` with subdirs `data/`, `uploads/`, `backups/`, `logs/`, `config/`.
- Generates `restaurant.db` and runs Flyway migrations (V1 → V18).
- Creates default admin (`admin / admin123`) and staff (`nhanvien01 / staff123`) accounts.
- Generates the QR PNG for pairing and writes it to `config/qr.png`.
- Starts the API on `0.0.0.0:8080`.
- Opens a system tray icon with "Open Dashboard", "Backup Now", "Open Data Folder", "Exit".
- On startup, the server initializes the `NotificationService` and registers its first `NotificationProvider` (currently a no-op `LoggingNotificationProvider`). To enable FCM push, place a service-account JSON in `config/fcm-service-account.json` and set `notification.fcm.enabled=true` in `config/server.properties` — no rebuild needed.

### Verify
- Browser: `http://<server-lan-ip>:8080/admin/` shows the dashboard.
- Browser: `http://<server-lan-ip>:8080/api/health` returns JSON.
- From an Android device on the same Wi-Fi: scan the QR on the dashboard, the app pairs automatically.

## Android

### Requirements
- Android 8.0+ (API 26) — required by FCM (Firebase Cloud Messaging) libraries
- Camera (for QR scan; manual entry fallback if absent)
- Same Wi-Fi as the server for first pairing
- **Internet access for push notifications** (FCM requires it; LAN-only operation is fully supported but pushes will only be received while the device has Internet)

### Install
1. Enable "Install from unknown sources" (Settings -> Apps -> Special access).
2. Install `RestaurantStaff.apk`.
3. Open the app -> scan QR -> login with admin/staff credentials.
4. On first login, the app requests notification permission (Android 13+) and registers an FCM device token with the server automatically.

## Updating

### Server
- Re-run the installer; it stops the existing server, replaces files, restarts.
- Database is preserved (lives under `data/` outside the install path).
- New Flyway migrations are applied automatically on first boot of the new version.

### Android
- Install the new APK over the existing one (signing key must match).
- On first launch after update, FCM token is re-registered; existing shifts / zones / checklists data is refetched from the server.

## Notification rollback / disable

If FCM becomes unavailable or rate-limited in production, disable it without rebuild:

```properties
# config/server.properties
notification.fcm.enabled=false
```

All business events still emit in-app notifications; only the FCM push side-effect is skipped. The system continues to work on LAN-only.

## FCM enable checklist (V2.3 / Phase E)

Use this checklist to turn push notifications on for a customer install. Each step is reversible without rebuilding.

1. **Create a Firebase project** (one-time, ~5 min)
   - Visit <https://console.firebase.google.com/>, click **Add project**, name it (e.g. `restaurant-prod`).
   - In **Project settings → Service accounts**, click **Generate new private key**. A JSON file downloads; rename it to `firebase-service-account.json`.
   - Note the `project_id` from the JSON — you'll need it for `restaurant.fcm.project-id`.

2. **Enable Firebase Cloud Messaging API**
   - In Google Cloud Console → **APIs & Services → Library**, search "Firebase Cloud Messaging API" and **Enable**.
   - Without this step, every send fails with `SENDER_ID_MISMATCH` or `PERMISSION_DENIED`.

3. **Add `google-services.json` to the Android app**
   - In Firebase Console → **Project settings → General → Your apps**, register an Android app with the package name from `RestaurantStaff.apk`.
   - Download `google-services.json` and rebuild the APK with it included under `android/app/`.
   - Skipping this step leaves the device unable to obtain an FCM token. The server still creates in-app notifications.

4. **Place the service-account JSON on the server**
   - Copy `firebase-service-account.json` to `%USERPROFILE%\RestaurantServer\config\`.
   - File name MUST match `restaurant.fcm.credentials-path` (default: `firebase-service-account.json`).

5. **Edit `server.properties`**
   ```properties
   restaurant.fcm.enabled=true
   restaurant.fcm.project-id=restaurant-prod        # from step 1
   restaurant.fcm.dry-run=false                      # true logs payloads but never calls Firebase
   restaurant.fcm.credentials-path=firebase-service-account.json
   restaurant.fcm.stale-after-days=180
   restaurant.fcm.retry-max-attempts=5
   restaurant.fcm.retry-max-per-sweep=50
   restaurant.fcm.retry-interval-ms=300000           # 5 min between retry sweeps
   restaurant.fcm.retry-initial-delay-ms=60000       # 1 min after server startup
   ```

6. **Restart the server** (`RestaurantServer.exe` from the Start Menu, or via the tray icon → Exit then re-launch).
   - On startup the log shows: `FirebaseApp initialised: name=[DEFAULT] projectId=restaurant-prod`.
   - If the service-account file is missing or malformed you'll see `FCM credentials not readable at '...'` and the noop provider takes over. Fix the file path, restart.

7. **Verify on a real device**
   - Open the Android app, go to **Settings → Server**, pair with the server, then log in.
   - From the admin dashboard, create a shift assignment for that user.
   - The device should receive a push within 2–5 seconds (depending on Google services latency).
   - In `logs/server.log`, look for `provider=fcm status=SENT providerMsgId=projects/.../messages/<id>`.

8. **Verify retry** (optional but recommended)
   - Temporarily set `restaurant.fcm.dry-run=true` for a few minutes. With `dry-run=true`, every push returns SKIPPED but the audit trail is intact.
   - Wait `restaurant.fcm.retry-interval-ms` (5 min default). Each previously-SKIPPED event stays SKIPPED because retry only re-attempts FAILED events, not SKIPPED. So this isn't a great retry test.
   - Better: stop the network for 60 s, assign a shift (it'll go FAILED), restore network, watch the retry sweep re-deliver it.

### Common FCM enable failures

| Symptom | Cause | Fix |
|---------|-------|-----|
| Log: `FCM credentials not readable at 'firebase-service-account.json'` | File missing or wrong path | Confirm the file exists at `%USERPROFILE%\RestaurantServer\config\firebase-service-account.json` |
| Log: `Failed to determine service account` | JSON file is corrupted or not a Firebase service account | Re-download from Firebase Console, don't edit the JSON |
| Push delivered to one device but not another | Different `google-services.json` baked into the APK vs. server project | Re-build APK with correct project |
| Log: `provider=fcm status=FAILED errorCode=UNREGISTERED` | User uninstalled the app or revoked notification permission | The token is auto-deactivated by `NotificationService`; no action needed |
| Log: `provider=fcm status=FAILED errorCode=SENDER_ID_MISMATCH` | Server project_id doesn't match the APK project | Verify `restaurant.fcm.project-id` matches `google-services.json` |
| Log: `provider=fcm status=FAILED errorCode=QUOTA_EXCEEDED` | Hit FCM free-tier daily limit (very high — should not happen for LAN-first) | Wait until the next UTC day or upgrade Firebase plan |

### Push performance characteristics (V2.3)

- **End-to-end latency (LAN server → device):** 1–5 s under normal conditions. Bounded by Google services upstream latency.
- **Retry schedule on transient failure:** 1 min → 5 min → 30 min → 2 h → 12 h. After 5 attempts the event stays FAILED permanently and the row is kept as audit trail.
- **Backlog sweep throughput:** `retry-max-per-sweep` (default 50) events per 5 min tick. A backlog of 1000 failed events clears in ~100 min.
- **Token rotation:** When a device re-registers with a new FCM token, the old row is automatically marked inactive (upsert by `user_id, token`).