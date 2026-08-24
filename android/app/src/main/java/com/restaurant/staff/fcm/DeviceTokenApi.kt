package com.restaurant.staff.fcm

import com.restaurant.staff.network.ApiEnvelopeDto
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * V2.3 / V18 — Device-token endpoints against the server.
 *
 * See `DeviceTokenController` on the server for the matching contract:
 *   POST /api/me/device-tokens  — idempotent upsert
 *   DELETE /api/me/device-tokens — unregister one token
 *
 * The response surfaces active count + platform so the UI can show a
 * "this device is registered for push" indicator in settings.
 */
interface DeviceTokenApi {

    @POST("api/me/device-tokens")
    suspend fun register(
        @Header("Authorization") bearer: String,
        @Body body: RegisterRequest
    ): ApiEnvelopeDto<RegisterResponse>

    @POST("api/me/device-tokens/delete")
    suspend fun unregister(
        @Header("Authorization") bearer: String,
        @Body body: UnregisterRequest
    ): ApiEnvelopeDto<RegisterResponse>
}

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val token: String,
    val platform: String,
    val deviceId: String?,
    val appVersion: String?
)

@JsonClass(generateAdapter = true)
data class UnregisterRequest(val token: String)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val registered: Boolean? = null,
    val active: Boolean? = null,
    val activeDeviceCount: Int? = null,
    val platform: String? = null
)