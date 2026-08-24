package com.restaurant.staff.fcm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * V2.3 / V18 — Resolves the OS-side permission state for {@code POST_NOTIFICATIONS}.
 *
 * Pre-Tiramisu (API < 33) the permission is granted at install time, so the
 * helper always returns {@link State.GRANTED}. On API 33+ we read
 * {@code PermissionChecker#checkSelfPermission} and translate to:
 *   - {@link State.GRANTED}   — system allowed us to post
 *   - {@link State.DENIED}    — user said no (or hasn't been asked yet, in
 *                              which case the UI shows the rationale dialog)
 *   - {@link State.NOT_APPLICABLE} — older OS, the UI hides the dialog
 *
 * This is the single source of truth for "should I show the user the
 * permission prompt?", so the UI layer never has to branch on Build.VERSION.
 */
object NotificationPermissionState {

    enum class State { GRANTED, DENIED, NOT_APPLICABLE }

    fun current(context: Context): State {
        // The constant was added in API 33. Anything below uses the legacy
        // "granted at install" model, so we lie and say GRANTED — the system
        // will deliver notifications without a runtime check.
        if (Build.VERSION.SDK_INT < 33) return State.GRANTED
        return if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) State.GRANTED else State.DENIED
    }

    /**
     * The string we register in ActivityResultContracts.RequestPermission.
     * Lives here so the UI layer doesn't have to duplicate the manifest key.
     */
    const val PERMISSION_NAME: String = Manifest.permission.POST_NOTIFICATIONS
}