package com.restaurant.staff.repository

import com.restaurant.staff.network.CheckInDto
import com.restaurant.staff.network.CheckInRequest
import com.restaurant.staff.network.ChecklistCompleteRequest
import com.restaurant.staff.network.ChecklistCompletionDto
import com.restaurant.staff.network.ChecklistViewDto
import com.restaurant.staff.network.ShiftAssignmentRespondRequest
import com.restaurant.staff.network.ShiftAssignmentViewDto
import com.restaurant.staff.network.ShiftViewDto
import com.restaurant.staff.network.V22Api
import com.restaurant.staff.network.ZoneAssignRequest
import com.restaurant.staff.network.ZoneAssignmentDto
import com.restaurant.staff.network.ZoneViewDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2.2 — repository wrapper for the new server endpoints. Kept deliberately
 * thin: it just forwards calls and unwraps the ApiResponse envelope so the
 * ViewModel layer can stay suspend / Result-free.
 */
@Singleton
class V22Repository @Inject constructor(private val api: V22Api) {

    suspend fun listShifts(): List<ShiftViewDto> = unwrap { api.myShifts() }

    suspend fun respondShift(id: Long, status: String, notes: String?): ShiftAssignmentViewDto {
        val req = ShiftAssignmentRespondRequest(status = status, notes = notes)
        return unwrap { api.respondShift(id, req) }
    }

    suspend fun listZones(): List<ZoneViewDto> = unwrap { api.myZones() }

    suspend fun currentZone(): ZoneAssignmentDto? = unwrap { api.myCurrentZone() }

    suspend fun zoneHistory(): List<ZoneAssignmentDto> = unwrap { api.myZoneHistory() }

    suspend fun selfAssignZone(userId: Long, zoneId: Long, reason: String?): ZoneAssignmentDto {
        val req = ZoneAssignRequest(userId = userId, zoneId = zoneId, reason = reason)
        return unwrap { api.selfAssignZone(req) }
    }

    suspend fun listChecklists(zoneId: Long?): List<ChecklistViewDto> = unwrap { api.myChecklists(zoneId) }

    suspend fun completeChecklistTask(
        taskId: Long,
        status: String,
        notes: String?,
        shiftId: Long?
    ): ChecklistCompletionDto {
        val req = ChecklistCompleteRequest(
            taskId = taskId, status = status, notes = notes, shiftId = shiftId)
        return unwrap { api.completeChecklistTask(req) }
    }

    suspend fun recentCompletions(limit: Int = 20): List<ChecklistCompletionDto> =
        unwrap { api.recentCompletions(limit) }

    suspend fun checkIn(zoneId: Long, action: String, notes: String?, deviceId: String?): CheckInDto {
        val req = CheckInRequest(zoneId = zoneId, action = action, notes = notes, deviceId = deviceId)
        return unwrap { api.checkIn(req) }
    }

    suspend fun recentCheckIns(limit: Int = 20): List<CheckInDto> =
        unwrap { api.recentCheckIns(limit) }

    private suspend inline fun <T> unwrap(block: () -> com.restaurant.staff.network.ApiEnvelopeDto<T>): T {
        val env = block()
        if (env.success != true) {
            throw IllegalStateException(env.error?.message ?: "API error")
        }
        return env.data ?: throw IllegalStateException("Empty response")
    }
}
