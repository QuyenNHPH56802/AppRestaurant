package com.restaurant.staff.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.2 — JSON-shape contract tests for the Android-side DTOs. Catches
 * regressions where the server renames a field and the Kotlin types don't
 * match the wire format anymore.
 *
 * <p>We use {@link KotlinJsonAdapterFactory} so the test runs without the
 * KSP-generated adapters — this matches the JVM unit test environment and
 * doesn't require the build pipeline to have run.</p>
 */
class V22DtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun shiftAssignmentViewDeserialisesServerShape() {
        val adapter = moshi.adapter(ShiftAssignmentViewDto::class.java)
        val json = """
            {
              "id": 7,
              "shiftId": 1,
              "shiftName": "Ca sáng",
              "shiftStartTime": "06:00",
              "shiftEndTime": "14:00",
              "userId": 3,
              "userName": "Nguyen Van A",
              "date": "2026-09-01",
              "status": "ACCEPTED",
              "notes": "ok"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)!!
        assertEquals(7L, dto.id)
        assertEquals("ACCEPTED", dto.status)
        assertEquals("2026-09-01", dto.date)
        assertEquals("Ca sáng", dto.shiftName)
    }

    @Test
    fun zoneViewWithTranslationsDeserialises() {
        val adapter = moshi.adapter(ZoneViewDto::class.java)
        val json = """
            {
              "id": 2,
              "code": "BEP_PHO",
              "color": "#EF4444",
              "status": "ACTIVE",
              "sortOrder": 1,
              "requiredStaff": 2,
              "currentAssignment": false,
              "translations": [
                {"lang": "vi", "name": "Bếp phở"},
                {"lang": "ko", "name": "쌀국수 주방"}
              ]
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)!!
        assertEquals("BEP_PHO", dto.code)
        assertEquals(2, dto.translations?.size)
        assertEquals("Bếp phở", dto.translations?.firstOrNull()?.name)
    }

    @Test
    fun checklistViewWithNestedTasksDeserialises() {
        val adapter = moshi.adapter(ChecklistViewDto::class.java)
        val json = """
            {
              "id": 5,
              "zoneId": 4,
              "zoneName": "Kho",
              "active": true,
              "sortOrder": 1,
              "translations": [{"lang": "vi", "title": "Danh sách kho"}],
              "tasks": [
                {"id": 10, "checklistId": 5, "required": true, "active": true, "sortOrder": 1,
                 "translations": [{"lang": "vi", "title": "Đóng cửa"}]}
              ]
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)!!
        assertEquals(1, dto.tasks?.size)
        assertEquals(true, dto.tasks?.first()?.required)
    }

    @Test
    fun checkInRequestSerialisesAsExpected() {
        val adapter = moshi.adapter(CheckInRequest::class.java)
        val req = CheckInRequest(zoneId = 4, action = "CHECK_IN", notes = "ok", deviceId = "dev")
        val json = adapter.toJson(req)
        assertTrue(json.contains("\"zoneId\":4"))
        assertTrue(json.contains("\"action\":\"CHECK_IN\""))
        assertTrue(json.contains("\"deviceId\":\"dev\""))
    }

    @Test
    fun shiftAssignmentRespondRequestSerialises() {
        val adapter = moshi.adapter(ShiftAssignmentRespondRequest::class.java)
        val req = ShiftAssignmentRespondRequest(status = "ACCEPTED", notes = null)
        val json = adapter.toJson(req)
        assertTrue(json.contains("\"status\":\"ACCEPTED\""))
    }

    @Test
    fun emptyEnvelopeIsSafe() {
        val adapter = moshi.adapter(ApiEnvelopeDto::class.java)
        val dto = adapter.fromJson("{}")
        assertNotNull(dto)
        assertEquals(null, dto.success)
        assertEquals(null, dto.data)
    }
}
