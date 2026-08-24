package com.restaurant.staff.fcm

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifPrefs: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

/**
 * V2.3 / V18 — Tracks whether the user has been prompted for the runtime
 * POST_NOTIFICATIONS permission.
 *
 * On API 33+ (Tiramisu) the OS shows the system dialog exactly once unless
 * the user explicitly ticks "don't ask again" in Settings. We do NOT want
 * our own in-app banner to ask more than once either, so we keep a flag.
 *
 * The flag is reset to false when the user logs out (see
 * [com.restaurant.staff.repository.AuthRepository]) so the next account
 * gets a fresh chance to opt-in.
 */
class NotificationPrefsStore(private val context: Context) {

    val prompted: Flow<Boolean> = context.notifPrefs.data.map { it[KEY_PROMPTED] ?: false }

    suspend fun current(): Boolean = prompted.first()

    suspend fun setPrompted(value: Boolean) {
        context.notifPrefs.edit { it[KEY_PROMPTED] = value }
    }

    suspend fun reset() = setPrompted(false)

    private companion object {
        val KEY_PROMPTED = booleanPreferencesKey("prompted")
    }
}