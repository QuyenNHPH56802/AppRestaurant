package com.restaurant.staff.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Server endpoints used during pairing (and a couple of public no-auth endpoints).
 * The auth-aware API is added in PHASE 6.
 */
interface ServerApi {

    @GET("api/health")
    suspend fun health(): ApiEnvelopeDto<HealthDto>

    @GET("api/server/info")
    suspend fun serverInfo(): ApiEnvelopeDto<ServerInfoDto>

    /**
     * Re-check connectivity with a 5s timeout. The Pairing screen calls this
     * after QR/manual entry.
     */
    @GET("api/health")
    suspend fun ping(@Query("lang") lang: String = "vi"): ApiEnvelopeDto<HealthDto>
}