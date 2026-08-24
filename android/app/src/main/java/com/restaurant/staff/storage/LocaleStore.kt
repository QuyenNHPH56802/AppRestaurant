package com.restaurant.staff.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.localeStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "app_locale")

/**
 * Stores the user-selected language (vi or ko). The Compose `AppRoot` reads this
 * on startup and applies it via the AndroidX per-app locales API.
 */
class LocaleStore(private val context: Context) {

    val language: Flow<String> = context.localeStore.data.map { p -> p[KEY] ?: "vi" }

    suspend fun set(lang: String) {
        val safe = if (lang == "ko") "ko" else "vi"
        context.localeStore.edit { p -> p[KEY] = safe }
    }

    companion object {
        private val KEY = stringPreferencesKey("language")
    }
}