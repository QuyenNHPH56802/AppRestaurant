package com.restaurant.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrPayloadParserTest {

    @Test
    fun parsesJsonPayload() {
        val raw = """{"server":"192.168.1.10","port":8080,"protocol":"http","version":"1.0.0"}"""
        val payload = QrPayloadParser.parse(raw)
        assertNotNull(payload)
        assertEquals("192.168.1.10", payload!!.server)
        assertEquals(8080, payload.port)
        assertEquals("http", payload.protocol)
        assertEquals("1.0.0", payload.version)
    }

    @Test
    fun parsesUrlPayload() {
        val payload = QrPayloadParser.parse("http://10.0.0.5:9000")
        assertNotNull(payload)
        assertEquals("10.0.0.5", payload!!.server)
        assertEquals(9000, payload.port)
        assertEquals("http", payload.protocol)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(QrPayloadParser.parse(""))
        assertNull(QrPayloadParser.parse("not a url or json"))
    }
}