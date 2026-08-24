package com.restaurant.staff.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * V2.2 — Android-side API for the new server endpoints (shifts, zones,
 * checklists, check-ins). Reuses the same OkHttp client as the rest of the
 * app so the JWT + locale headers are applied uniformly.
 */

// ----------------- Shift -----------------

@JsonClass(generateAdapter = true)
data class ShiftViewDto(
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val tz: String? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null
)

@JsonClass(generateAdapter = true)
data class ShiftAssignmentViewDto(
    val id: Long? = null,
    val shiftId: Long? = null,
    val shiftName: String? = null,
    val shiftStartTime: String? = null,
    val shiftEndTime: String? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val date: String? = null,
    val status: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ShiftAssignmentRespondRequest(
    val status: String,
    val notes: String? = null
)

// ----------------- Zone -----------------

@JsonClass(generateAdapter = true)
data class ZoneTranslationDto(
    val lang: String? = null,
    val name: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ZoneViewDto(
    val id: Long? = null,
    val code: String? = null,
    val color: String? = null,
    val status: String? = null,
    val sortOrder: Int? = null,
    val requiredStaff: Int? = null,
    val currentAssignment: Boolean? = null,
    val translations: List<ZoneTranslationDto>? = null
)

@JsonClass(generateAdapter = true)
data class ZoneAssignmentDto(
    val id: Long? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val zoneId: Long? = null,
    val zoneCode: String? = null,
    val zoneName: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val current: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ZoneAssignRequest(
    val userId: Long,
    val zoneId: Long,
    val reason: String? = null
)

// ----------------- Checklist -----------------

@JsonClass(generateAdapter = true)
data class ChecklistTaskTranslationDto(
    val lang: String? = null,
    val title: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ChecklistTaskDto(
    val id: Long? = null,
    val checklistId: Long? = null,
    val required: Boolean? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null,
    val translations: List<ChecklistTaskTranslationDto>? = null
)

@JsonClass(generateAdapter = true)
data class ChecklistTranslationDto(
    val lang: String? = null,
    val title: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ChecklistViewDto(
    val id: Long? = null,
    val zoneId: Long? = null,
    val zoneCode: String? = null,
    val zoneName: String? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null,
    val translations: List<ChecklistTranslationDto>? = null,
    val tasks: List<ChecklistTaskDto>? = null
)

@JsonClass(generateAdapter = true)
data class ChecklistCompleteRequest(
    val taskId: Long,
    val status: String,
    val notes: String? = null,
    val photoUrl: String? = null,
    val shiftId: Long? = null
)

@JsonClass(generateAdapter = true)
data class ChecklistCompletionDto(
    val id: Long? = null,
    val taskId: Long? = null,
    val checklistId: Long? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val status: String? = null,
    val notes: String? = null,
    val photoUrl: String? = null,
    val shiftId: Long? = null,
    val completedAt: String? = null
)

// ----------------- Check-in -----------------

@JsonClass(generateAdapter = true)
data class CheckInRequest(
    val zoneId: Long,
    val action: String,
    val notes: String? = null,
    val deviceId: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckInDto(
    val id: Long? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val zoneId: Long? = null,
    val zoneCode: String? = null,
    val action: String? = null,
    val notes: String? = null,
    val deviceId: String? = null,
    val createdAt: String? = null
)

// ----------------- API -----------------

interface V22Api {

    @GET("api/me/shifts")
    suspend fun myShifts(): ApiEnvelopeDto<List<ShiftAssignmentViewDto>>

    @POST("api/me/shifts/{id}/respond")
    suspend fun respondShift(
        @Path("id") id: Long,
        @Body req: ShiftAssignmentRespondRequest
    ): ApiEnvelopeDto<ShiftAssignmentViewDto>

    @GET("api/me/zones")
    suspend fun myZones(): ApiEnvelopeDto<List<ZoneViewDto>>

    @GET("api/me/zones/current")
    suspend fun myCurrentZone(): ApiEnvelopeDto<ZoneAssignmentDto?>

    @GET("api/me/zones/history")
    suspend fun myZoneHistory(): ApiEnvelopeDto<List<ZoneAssignmentDto>>

    @POST("api/me/zones/assign")
    suspend fun selfAssignZone(@Body req: ZoneAssignRequest): ApiEnvelopeDto<ZoneAssignmentDto>

    @GET("api/me/checklists")
    suspend fun myChecklists(@Query("zoneId") zoneId: Long? = null): ApiEnvelopeDto<List<ChecklistViewDto>>

    @POST("api/me/checklists/complete")
    suspend fun completeChecklistTask(@Body req: ChecklistCompleteRequest): ApiEnvelopeDto<ChecklistCompletionDto>

    @GET("api/me/checklists/completions")
    suspend fun recentCompletions(@Query("limit") limit: Int = 20): ApiEnvelopeDto<List<ChecklistCompletionDto>>

    @POST("api/me/check-ins")
    suspend fun checkIn(@Body req: CheckInRequest): ApiEnvelopeDto<CheckInDto>

    @GET("api/me/check-ins")
    suspend fun recentCheckIns(@Query("limit") limit: Int = 20): ApiEnvelopeDto<List<CheckInDto>>
}
