package com.restaurant.staff.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Auth + session endpoints. Body and response use Moshi-generated adapters.
 */
interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelopeDto<LoginResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String): ApiEnvelopeDto<LogoutResponseDto>

    @GET("api/me")
    suspend fun me(@Header("Authorization") bearer: String): ApiEnvelopeDto<UserSummaryDto>
}

@JsonClass(generateAdapter = true)
data class LoginRequest(val username: String, val password: String)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    val token: String? = null,
    val expiresInSeconds: Long? = null,
    val user: UserSummaryDto? = null
)

@JsonClass(generateAdapter = true)
data class LogoutResponseDto(val message: String? = null)

@JsonClass(generateAdapter = true)
data class UserSummaryDto(
    val id: Long? = null,
    val username: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val lang: String? = null
)