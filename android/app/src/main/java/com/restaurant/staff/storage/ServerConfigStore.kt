package com.restaurant.staff.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.serverConfigStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "server_config")

/**
 * Persists the active server base URL on the device. Written by PairingScreen,
 * read by every API client.
 */
class ServerConfigStore(private val context: Context) {

    private val keyHost = stringPreferencesKey("host")
    private val keyPort = intPreferencesKey("port")
    private val keyProtocol = stringPreferencesKey("protocol")
    private val keyVersion = stringPreferencesKey("server_version")
    private val keyConfigured = stringPreferencesKey("configured_at")

    val config: Flow<ServerConfig?> = context.serverConfigStore.data.map { p ->
        val host = p[keyHost] ?: return@map null
        val port = p[keyPort] ?: 8080
        val protocol = p[keyProtocol] ?: "http"
        ServerConfig(host, port, protocol, p[keyVersion], p[keyConfigured])
    }

    suspend fun current(): ServerConfig? = config.first()

    suspend fun save(config: ServerConfig) {
        context.serverConfigStore.edit { p ->
            p[keyHost] = config.host
            p[keyPort] = config.port
            p[keyProtocol] = config.protocol
            config.serverVersion?.let { p[keyVersion] = it } ?: p.remove(keyVersion)
            p[keyConfigured] = System.currentTimeMillis().toString()
        }
    }

    suspend fun clear() {
        context.serverConfigStore.edit { it.clear() }
    }

    fun baseUrl(config: ServerConfig): String =
        "${config.protocol}://${config.host}:${config.port}"
}

data class ServerConfig(
    val host: String,
    val port: Int,
    val protocol: String = "http",
    val serverVersion: String? = null,
    val configuredAtIso: String? = null
)