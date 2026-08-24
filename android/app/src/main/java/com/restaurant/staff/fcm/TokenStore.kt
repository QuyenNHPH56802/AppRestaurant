package com.restaurant.staff.fcm

import android.content.Context
import android.util.Log as AndroidLog
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fcmTokenStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "fcm_token")

/**
 * V2.3 / V18 — Persists the most recent FCM token.
 *
 * The single source of truth for "what does the device think its current
 * token is". The [TokenRotator] compares incoming tokens from
 * [RestaurantFcmService.onNewToken] against the value stored here, and only
 * posts to the server when they differ. That avoids the
 * rotate-on-every-cold-start cost that would otherwise dominate server logs.
 *
 * This DataStore is intentionally separate from the session DataStore so a
 * logout does not erase the FCM token — a relogin on the same device should
 * keep push working without a fresh FCM rotation.
 */
class TokenStore(private val context: Context) {

    val token: Flow<String?> = context.fcmTokenStore.data.map { it[KEY_TOKEN] }
    val deviceId: Flow<String?> = context.fcmTokenStore.data.map { it[KEY_DEVICE_ID] }

    suspend fun currentToken(): String? = token.first()
    suspend fun currentDeviceId(): String? = deviceId.first()

    suspend fun save(token: String, deviceId: String) {
        context.fcmTokenStore.edit { p ->
            p[KEY_TOKEN] = token
            p[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun clear() {
        context.fcmTokenStore.edit { it.remove(KEY_TOKEN); it.remove(KEY_DEVICE_ID) }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }
}

internal object TokenStoreExt {
    /** Internal helper used by [RestaurantFcmService] which doesn't DI. */
    suspend fun put(context: Context, token: String) {
        val store = TokenStore(context)
        val existing = store.currentToken()
        val deviceId = store.currentDeviceId()
            ?: android.provider.Settings.Secure.getString(
                context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown-device"
        if (existing != token) {
            store.save(token, deviceId)
            AndroidLog.i("TokenStore", "saved new fcm token (length=${token.length})")
        } else {
            AndroidLog.d("TokenStore", "fcm token unchanged; not re-saving")
        }
    }
}