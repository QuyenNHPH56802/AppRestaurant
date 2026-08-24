package com.restaurant.staff.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.3 / V18 — Pure-function tests for [NotificationPayload.parseVerdict].
 *
 * The payload is server-controlled JSON so we mostly guard against the
 * common client bugs:
 *   - missing / null payload (don't crash)
 *   - the response key written in a different order than we expect
 *   - extra whitespace before/after the colon
 *   - other fields like {@code shiftId} that look similar
 */
class NotificationPayloadTest {

    @Test
    fun parsesAcceptedVerdict() {
        val payload = """{"shiftId":42,"response":"ACCEPTED","respondedAt":"2026-08-24T01:00:00Z"}"""
        assertEquals("ACCEPTED", NotificationPayload.parseVerdict(payload))
    }

    @Test
    fun parsesDeclinedVerdict() {
        val payload = """{"response":"DECLINED"}"""
        assertEquals("DECLINED", NotificationPayload.parseVerdict(payload))
    }

    @Test
    fun parsesWithExtraWhitespace() {
        val payload = """{ "response" : "ACCEPTED" }"""
        assertEquals("ACCEPTED", NotificationPayload.parseVerdict(payload))
    }

    @Test
    fun returnsNullForMissingKey() {
        val payload = """{"shiftId":42,"respondedAt":"2026-08-24T01:00:00Z"}"""
        assertNull(NotificationPayload.parseVerdict(payload))
    }

    @Test
    fun returnsNullForBlankOrNull() {
        assertNull(NotificationPayload.parseVerdict(null))
        assertNull(NotificationPayload.parseVerdict(""))
        assertNull(NotificationPayload.parseVerdict("   "))
    }

    @Test
    fun doesNotMatchArbitraryString() {
        // Garbage verdict that the server would have rejected; client must
        // still treat it as "no verdict" rather than crash.
        val payload = """{"response":"FOO"}"""
        assertNull(NotificationPayload.parseVerdict(payload))
    }

    @Test
    fun validVerdictsSetContainsExpected() {
        assertTrue(NotificationPayload.VALID_VERDICTS.contains("ACCEPTED"))
        assertTrue(NotificationPayload.VALID_VERDICTS.contains("DECLINED"))
        assertFalse(NotificationPayload.VALID_VERDICTS.contains("MAYBE"))
    }
}