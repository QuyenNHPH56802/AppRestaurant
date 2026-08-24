package com.restaurant.staff.fcm

import android.content.Context
import android.os.Build
import android.util.Log as AndroidLog
import com.google.firebase.messaging.FirebaseMessaging
import com.restaurant.staff.BuildConfig
import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.storage.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2.3 / V18 — Bridges FCM token rotation events to the server.
 *
 * Two trigger points:
 *   1. {@code registerAfterLogin} — called by [com.restaurant.staff.repository.AuthRepository]
 *      immediately after a successful login. We fetch the token from FCM and
 *      POST it. The server's upsert is idempotent on (userId, token), so a
 *      repeat call is free.
 *   2. {@code onRotation} — called by [RestaurantFcmService.onNewToken]. The
 *      FCM SDK hands us a new token (rotate on first install, after data
 *      reset, or when the user toggles Google Play Services). We POST again,
 *      keyed by the current JWT if any.
 *
 * Idempotency: we only POST when the in-memory + DataStore value differs
 * from the freshly-fetched FCM token. This protects the server from a
 * per-cold-start fan-out of identical token POSTs.
 *
 * Errors: never crash. Any failure is logged and swallowed; the next
 * rotation or the next login will retry.
 */
@Singleton
class TokenRotator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClientProvider,
    private val session: SessionStore,
    private val tokenStore: TokenStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * One-shot after login. Skipped when FCM is not configured
     * (BuildConfig blank, or Google Play Services unavailable).
     */
    fun registerAfterLogin() {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                sendToServer(token, active = true)
            } catch (ex: Throwable) {
                AndroidLog.w(TAG, "registerAfterLogin failed: ${ex.javaClass.simpleName}: ${ex.message}")
            }
        }
    }

    /**
     * Called from [RestaurantFcmService.onNewToken]. POSTs the token to the
     * server if and only if it changed since the last call.
     */
    fun onRotation(newToken: String) {
        scope.launch {
            try {
                sendToServer(newToken, active = true)
            } catch (ex: Throwable) {
                AndroidLog.w(TAG, "onRotation failed: ${ex.javaClass.simpleName}: ${ex.message}")
            }
        }
    }

    /**
     * Called from [com.restaurant.staff.repository.AuthRepository.logout].
     * The server's deactivate keeps the row but flips isActive=0; next
     * registerAfterLogin on the same device flips it back to 1.
     */
    fun unregisterOnLogout() {
        scope.launch {
            try {
                val token = tokenStore.currentToken() ?: return@launch
                val jwt = session.currentToken() ?: return@launch
                api.deviceTokenApi().unregister("Bearer $jwt", UnregisterRequest(token))
            } catch (ex: Throwable) {
                AndroidLog.w(TAG, "unregisterOnLogout failed: ${ex.javaClass.simpleName}: ${ex.message}")
            }
        }
    }

    private suspend fun sendToServer(token: String, active: Boolean) {
        if (!RestaurantFirebaseApp.isConfigured) {
            AndroidLog.i(TAG, "FCM not configured; skipping token POST (length=${token.length})")
            return
        }
        val previous = tokenStore.currentToken()
        if (!TokenRotationGuard.shouldPost(previous, token)) {
            AndroidLog.d(TAG, "token unchanged; not POSTing again")
            return
        }
        val jwt = session.currentToken()
        if (jwt.isNullOrBlank()) {
            // Save locally so the next registerAfterLogin picks it up. The
            // server-side registration happens later (after the user logs in).
            tokenStore.save(token, currentDeviceId())
            AndroidLog.i(TAG, "no JWT yet; stored token locally for later registration")
            return
        }
        val req = RegisterRequest(
            token = token,
            platform = "ANDROID",
            deviceId = currentDeviceId(),
            appVersion = BuildConfig.VERSION_NAME
        )
        val resp = api.deviceTokenApi().register("Bearer $jwt", req)
        if (resp.success == true) {
            tokenStore.save(token, currentDeviceId())
            AndroidLog.i(TAG, "device token registered; server activeCount=${resp.data?.activeDeviceCount}")
        } else {
            AndroidLog.w(TAG, "device token register failed: ${resp.error?.code} ${resp.error?.message}")
        }
    }

    private suspend fun currentDeviceId(): String {
        tokenStore.currentDeviceId()?.let { return it }
        return try {
            @Suppress("HardwareIds")
            android.provider.Settings.Secure.getString(
                context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown-device"
        } catch (ex: Throwable) {
            "unknown-device"
        }
    }

    companion object {
        private const val TAG = "TokenRotator"
    }
}