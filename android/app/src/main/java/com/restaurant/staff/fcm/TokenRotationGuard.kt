package com.restaurant.staff.fcm

/**
 * V2.3 / V18 — Decides whether a freshly-fetched FCM token should be POSTed
 * to the server.
 *
 * Extracted as a pure function so we can unit-test it without Robolectric.
 * The full [TokenRotator] still owns the network + DataStore side, but the
 * decision ("is this a real change worth a server round-trip?") lives here.
 *
 * Rules:
 *   - First call after install: no previous token. Always POST.
 *   - Same token as last time: skip. The OS occasionally re-delivers an
 *     unchanged token; we must not spam the server.
 *   - New token: POST.
 *   - Forced (e.g. after explicit logout + login): always POST, even if the
 *     token hasn't changed, so the server re-activates a row that may have
 *     been deactivated during the gap.
 */
object TokenRotationGuard {

    fun shouldPost(
        previous: String?,
        current: String,
        forced: Boolean = false
    ): Boolean {
        if (forced) return true
        if (previous == null) return true
        if (previous == current) return false
        return true
    }
}