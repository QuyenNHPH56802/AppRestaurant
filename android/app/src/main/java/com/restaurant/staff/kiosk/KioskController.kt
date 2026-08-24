package com.restaurant.staff.kiosk

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import com.restaurant.staff.storage.KioskSettingsStore

/**
 * PHASE 12 controller for Kiosk Mode.
 *
 * Two modes are supported:
 *   1. **Lock Task Mode (preferred)** — requires the app to be a Device Owner.
 *      A short provisioning guide lives in `docs/kiosk-provisioning.md` (one-time
 *      `adb shell dpm set-device-owner` step). Once the app is Device Owner, it
 *      can call [startLockTask] to enter kiosk and [stopLockTask] to exit.
 *
 *   2. **Screen Pinning (fallback)** — works without root or Device Owner.
 *      The user is shown Android's system screen-pinning prompt and must confirm
 *      ("Start") to begin. To exit, the PIN screen requires entering the admin
 *      PIN, then `stopLockTask()` is called from the app which surfaces the
 *      standard unpin confirmation.
 */
class KioskController(
    private val context: Context,
    private val store: KioskSettingsStore
) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName =
        ComponentName(context, RestaurantAdminReceiver::class.java)

    /** True when the app is the active Device Owner. */
    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /** True when the system supports Lock Task on this app. */
    val isLockTaskPermitted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dpm.isLockTaskPermitted(context.packageName)
        } else true

    /** Begin Lock Task on the current activity. Requires Device Owner + permitted. */
    fun startLockTask(activity: Activity) {
        // Hide status bar by making the app full-screen (best-effort)
        runCatching {
            activity.window.insetsController?.hide(
                android.view.WindowInsets.Type.statusBars()
            )
        }
        activity.startLockTask()
    }

    /** End Lock Task. Surfaces the system unpin screen. */
    fun stopLockTask(activity: Activity) {
        runCatching { activity.stopLockTask() }
    }

    /** Begin Screen Pinning (fallback path). */
    fun startScreenPinning(activity: Activity) {
        activity.startLockTask()
    }

    /** End Screen Pinning. */
    fun stopScreenPinning(activity: Activity) {
        runCatching { activity.stopLockTask() }
    }

    /** Whether the user is currently allowed to disable Lock Task via the OS UI. */
    fun setLockTaskFeatures(features: Int) {
        if (!isDeviceOwner) return
        dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setLockTaskFeatures(adminComponent, features)
        }
    }

    /** Suspends the user so they cannot quickly escape via factory reset shortcuts. */
    fun applyKioskRestrictions() {
        if (!isDeviceOwner) return
        // Block adding/removing users while in kiosk mode.
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        runCatching { dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER) }
        runCatching { dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_USER) }
        runCatching { dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET) }
    }

    fun clearKioskRestrictions() {
        if (!isDeviceOwner) return
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        runCatching { dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER) }
        runCatching { dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_USER) }
        runCatching { dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET) }
    }

    /** Verify the admin PIN typed by the user against the stored value. */
    suspend fun verifyPin(input: String): Boolean {
        val stored = store.currentPin()
        return !stored.isNullOrBlank() && stored == input
    }
}

/**
 * Empty DeviceAdminReceiver. Required for the app to be promoted to Device Owner
 * via `adb shell dpm set-device-owner com.restaurant.staff/.kiosk.RestaurantAdminReceiver`.
 * No policies are enforced from this receiver — Lock Task is the policy.
 */
class RestaurantAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        // Device Owner is now active.
    }

    @Suppress("DEPRECATION")
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Restaurant Staff Device Owner is required for Kiosk Mode."
}