package com.restaurant.staff.network

import com.restaurant.staff.fcm.DeviceTokenApi
import com.restaurant.staff.notifications.NotificationsApi
import com.restaurant.staff.storage.ServerConfig
import com.restaurant.staff.storage.ServerConfigStore
import com.restaurant.staff.storage.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Lazy provider of the current Retrofit instance. Rebuilt whenever the saved
 * server config changes. We use runBlocking on init only when actually needed
 * (e.g. when ApiService is requested before the splash decision completes);
 * the Pairing screen resolves the config ahead of time.
 */
class ApiClientProvider(
    private val configStore: ServerConfigStore,
    private val sessionStore: SessionStore,
    private val debug: Boolean = false
) {
    @Volatile private var currentBaseUrl: String? = null
    @Volatile private var retrofit: Retrofit? = null

    val moshi = NetworkClient.moshi()

    val client: OkHttpClient by lazy {
        NetworkClient.okHttp(debug).newBuilder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .build()
    }

    fun serverApi(): ServerApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set; complete pairing first.") }
    ).create(ServerApi::class.java)

    fun authApi(): AuthApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set; complete pairing first.") }
    ).create(AuthApi::class.java)

    fun categoryApi(): CategoryApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(CategoryApi::class.java)

    fun foodApi(): FoodApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(FoodApi::class.java)

    fun storeApi(): StoreApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(StoreApi::class.java)

    fun deviceTokenApi(): DeviceTokenApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(DeviceTokenApi::class.java)

    fun notificationsApi(): NotificationsApi = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(NotificationsApi::class.java)

    fun v22Api(): V22Api = retrofitFor(
        runBlocking { configStore.current() ?: error("ServerConfig not set") }
    ).create(V22Api::class.java)

    fun serverApiFor(config: ServerConfig): ServerApi = retrofitFor(config).create(ServerApi::class.java)

    private fun retrofitFor(config: ServerConfig): Retrofit {
        val base = configStore.baseUrl(config)
        val existing = retrofit
        if (existing != null && currentBaseUrl == base) return existing
        val r = NetworkClient.retrofit(client, base, moshi)
        currentBaseUrl = base
        retrofit = r
        return r
    }
}