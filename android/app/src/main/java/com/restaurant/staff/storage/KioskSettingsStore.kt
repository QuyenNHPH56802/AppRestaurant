package com.restaurant.staff.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kioskDataStore by preferencesDataStore("kiosk_settings")

/**
 * PHASE 12. Persists Kiosk Mode settings: enabled flag and admin PIN (stored
 * as plain text locally for simplicity — kiosk PINs are device-local and
 * never sent to the server). For higher-security deployments, swap to a
 * EncryptedSharedPreferences (AndroidX Security) without changing the API.
 */
@Singleton
class KioskSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val enabledKey = booleanPreferencesKey("kiosk_enabled")
    private val pinKey = stringPreferencesKey("kiosk_admin_pin")

    val enabled: Flow<Boolean> = context.kioskDataStore.data.map { it[enabledKey] ?: false }
    val adminPin: Flow<String?> = context.kioskDataStore.data.map { it[pinKey] }

    suspend fun setEnabled(value: Boolean) {
        context.kioskDataStore.edit { it[enabledKey] = value }
    }

    suspend fun setPin(value: String) {
        context.kioskDataStore.edit { it[pinKey] = value }
    }

    suspend fun currentEnabled(): Boolean = enabled.first()
    suspend fun currentPin(): String? = adminPin.first()
}