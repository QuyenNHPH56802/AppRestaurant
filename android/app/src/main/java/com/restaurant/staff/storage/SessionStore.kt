package com.restaurant.staff.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restaurant.staff.network.UserSummaryDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Persists the JWT and the current user. Hilt provides this as a singleton.
 * The JWT is used in the OkHttp AuthInterceptor; user is read by Profile/Home.
 */
class SessionStore(private val context: Context) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(UserSummaryDto::class.java)

    val token: Flow<String?> = context.sessionStore.data.map { p -> p[KEY_TOKEN] }
    val user: Flow<UserSummaryDto?> = context.sessionStore.data.map { p ->
        p[KEY_USER]?.let { runCatching { userAdapter.fromJson(it) }.getOrNull() }
    }

    suspend fun currentToken(): String? = token.first()

    suspend fun save(token: String, user: UserSummaryDto) {
        context.sessionStore.edit { p ->
            p[KEY_TOKEN] = token
            p[KEY_USER] = userAdapter.toJson(user)
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { p ->
            p.remove(KEY_TOKEN)
            p.remove(KEY_USER)
        }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER = stringPreferencesKey("user")
    }
}