package com.restaurant.staff.notifications

import com.restaurant.staff.network.ApiEnvelopeDto
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * V2.3 / V18 — In-app notification feed endpoints.
 *
 * Mirrors {@code MeNotificationController} on the server. The server
 * pre-renders titles/bodies in vi + ko at write time, so the client just
 * picks the matching column via {@code ?lang=}.
 */
interface NotificationsApi {

    @GET("api/me/notifications")
    suspend fun list(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("lang") lang: String = "vi"
    ): ApiEnvelopeDto<NotificationListResponseDto>

    @GET("api/me/notifications/unread-count")
    suspend fun unreadCount(
        @Header("Authorization") bearer: String
    ): ApiEnvelopeDto<UnreadCountResponseDto>

    @POST("api/me/notifications/{id}/read")
    suspend fun markRead(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): ApiEnvelopeDto<Map<String, Any>>

    @POST("api/me/notifications/read-all")
    suspend fun markAllRead(
        @Header("Authorization") bearer: String
    ): ApiEnvelopeDto<Map<String, Any>>

    @GET("api/me/notifications/{id}/events")
    suspend fun events(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): ApiEnvelopeDto<List<NotificationEventViewDto>>

    /**
     * V2.3 / V18 — User accepts / declines a notification (typically
     * SHIFT_ASSIGNED). Idempotent on the server side; re-calling with the
     * same verdict just refreshes the respondedAt timestamp.
     */
    @POST("api/me/notifications/{id}/respond")
    suspend fun respond(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Body body: RespondRequest
    ): ApiEnvelopeDto<RespondResponseDto>
}

@JsonClass(generateAdapter = true)
data class RespondRequest(val verdict: String)

@JsonClass(generateAdapter = true)
data class RespondResponseDto(
    val id: Long? = null,
    val verdict: String? = null,
    val respondedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationViewDto(
    val id: Long? = null,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val payloadJson: String? = null,
    val readAt: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationListResponseDto(
    val items: List<NotificationViewDto>? = null,
    val page: Int? = null,
    val size: Int? = null,
    val total: Long? = null,
    val totalPages: Int? = null
)

@JsonClass(generateAdapter = true)
data class UnreadCountResponseDto(val count: Long? = null)

@JsonClass(generateAdapter = true)
data class NotificationEventViewDto(
    val id: Long? = null,
    val notificationId: Long? = null,
    val channel: String? = null,
    val status: String? = null,
    val provider: String? = null,
    val providerMsgId: String? = null,
    val errorCode: String? = null,
    val attempts: Int? = null,
    val lastAttemptAt: String? = null,
    val createdAt: String? = null
)