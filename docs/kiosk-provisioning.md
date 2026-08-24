# Kiosk Mode — provisioning & operations

PHASE 12. Two-tier strategy is supported: **Device Owner (preferred)** for dedicated tablets and **Screen Pinning (fallback)** for consumer devices that can't be enrolled.

## 1. Tier A — Device Owner (production-grade)

Used when the customer's devices are dedicated restaurant tablets. Provides true single-app lock with home button suppression and uninstall protection.

### One-time provisioning (per device)

1. Factory-reset the device (so no accounts are pre-added — required for `dpm set-device-owner`).
2. Connect via USB, enable **USB debugging** on the device.
3. Install the APK from a build machine with `adb`:

   ```bash
   adb install -r RestaurantStaff.apk
   adb shell dpm set-device-owner com.restaurant.staff/.kiosk.RestaurantAdminReceiver
   adb shell am start -n com.restaurant.staff/.MainActivity
   ```

4. In the app:
   - Open **Settings → Kiosk**
   - Tap **Set PIN** → choose a 4–8 digit admin PIN
   - Tap the **Enable Kiosk Mode** switch

The app calls `startLockTask()`, applies user restrictions (`DISALLOW_ADD_USER`, `DISALLOW_REMOVE_USER`, `DISALLOW_FACTORY_RESET`), and the device is now locked to Restaurant Staff.

### Exiting Kiosk Mode

From inside the app: **Profile → Thoát chế độ Kiosk** → enter admin PIN → app calls `stopLockTask()` and disables the flag.

From `adb` (only when the device is reachable):

```bash
adb shell dpm remove-active-admin com.restaurant.staff/.kiosk.RestaurantAdminReceiver
adb uninstall com.restaurant.staff
```

### Behavior summary

- Home, Back, and Recents keys are intercepted.
- Status bar is hidden via `WindowInsetsController.hide(statusBars())`.
- App uninstall via the Settings UI is blocked.
- A hard reboot re-enters Lock Task automatically (MainActivity checks the persisted flag and re-calls `startLockTask()` in `onCreate`).

## 2. Tier B — Screen Pinning (fallback)

Used when the customer's devices are off-the-shelf consumer phones/tablets and cannot be enrolled as Device Owner.

### Setup

1. Install the APK on the device.
2. Open the app, complete pairing + login.
3. **Settings → Kiosk → Set PIN** → choose an admin PIN.
4. **Settings → Kiosk → Enable** → confirm the system "Start" screen-pinning prompt.

The app calls `startLockTask()`. The Home key is suppressed only while this app is foregrounded.

### Exiting

- From the app: **Profile → Thoát chế độ Kiosk** → enter admin PIN.
- Outside the app: hold Back + Recents (depending on Android version) to show the unpin screen, then enter the admin PIN.

### Known limitations

- Power + volume-down factory reset can bypass the lock. The customer is informed in training.
- The app must be foregrounded for the kiosk to be effective.

## 3. Admin PIN

- 4–8 digit numeric.
- Stored locally on the device in DataStore.
- Required to enable Kiosk Mode and to exit it.
- Reset: re-install the APK (loses local Kiosk state). On Tier A devices, you can also `adb shell pm clear com.restaurant.staff`.

## 4. Verification checklist

| Step | Tier A | Tier B |
| --- | --- | --- |
| Home key is intercepted | ✅ | ✅ while app is foreground |
| Status bar hidden | ✅ | ❌ (system-level, requires owner) |
| Re-launches into Lock Task after reboot | ✅ | ❌ |
| Cannot uninstall from Settings | ✅ | ❌ |
| Admin PIN exit works | ✅ | ✅ |
| Hard reset bypasses | ❌ | ⚠️ (vol-down) |

## 5. UX flow

```
Settings
  └─ Kiosk
       ├─ Set / Change admin PIN
       └─ Enable Kiosk ── enter PIN ── confirm system prompt
Profile
  └─ Thoát chế độ Kiosk ── enter PIN ── exit Lock Task
```