package com.restaurant.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2.3 / V18 — Unit tests for the JSON payload merge / extract logic used
 * by {@link NotificationService#respond} and {@link NotificationService#readResponse}.
 *
 * Those private methods are exercised through reflection so we don't have to
 * spin up a Spring context just to verify the JSON shape. The behaviour
 * we're protecting:
 *   - existing payload fields are preserved (don't drop {@code shiftId})
 *   - {@code response} and {@code respondedAt} are stamped on every call
 *   - malformed JSON doesn't drop the existing payload — it gets moved
 *     under a {@code _legacy} key so the operator can still read it
 *   - {@code extractResponse} handles null, blank, malformed, and missing
 *     {@code response} keys without throwing
 */
class NotificationPayloadMergeTest {

    private NotificationService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // Construct the service with null collaborators — we never invoke them
        // for these unit tests. We only call the private JSON helpers via
        // reflection so the dependencies are irrelevant.
        service = new NotificationService(
                null, null, null, null, null, null, null, null, mapper
        );
    }

    @Test
    void mergeAddsResponseAndPreservesExistingFields() throws Exception {
        String input = "{\"shiftId\":42,\"date\":\"2026-08-24\"}";
        String merged = invokeMerge(input, "ACCEPTED");
        assertNotNull(merged);
        @SuppressWarnings("unchecked")
        var map = mapper.readValue(merged, java.util.Map.class);
        assertEquals("ACCEPTED", map.get("response"));
        assertEquals(42, map.get("shiftId"));
        assertEquals("2026-08-24", map.get("date"));
        assertNotNull(map.get("respondedAt"));
    }

    @Test
    void mergeIntoNullOrBlankProducesCleanJson() throws Exception {
        String merged = invokeMerge(null, "DECLINED");
        @SuppressWarnings("unchecked")
        var map = mapper.readValue(merged, java.util.Map.class);
        assertEquals("DECLINED", map.get("response"));
        assertNotNull(map.get("respondedAt"));
        assertEquals(2, map.size());
    }

    @Test
    void mergeOnMalformedJsonPreservesLegacy() throws Exception {
        String malformed = "{not really json";
        String merged = invokeMerge(malformed, "ACCEPTED");
        @SuppressWarnings("unchecked")
        var map = mapper.readValue(merged, java.util.Map.class);
        assertEquals("ACCEPTED", map.get("response"));
        assertEquals(malformed, map.get("_legacy"));
    }

    @Test
    void mergeThenExtractRoundTrip() throws Exception {
        for (String v : new String[]{"ACCEPTED", "DECLINED"}) {
            String merged = invokeMerge(null, v);
            String extracted = invokeExtract(merged);
            assertEquals(v, extracted);
        }
    }

    @Test
    void extractHandlesNullAndBlank() throws Exception {
        assertNull(invokeExtract(null));
        assertNull(invokeExtract(""));
        assertNull(invokeExtract("   "));
    }

    @Test
    void extractReturnsNullForMissingKey() throws Exception {
        String payload = "{\"shiftId\":42}";
        assertNull(invokeExtract(payload));
    }

    @Test
    void extractTolerantOfMalformedJson() throws Exception {
        // We never want a malformed payload to surface a 500 to the client.
        assertNull(invokeExtract("{not really"));
    }

    // --- reflection helpers --------------------------------------------------

    private String invokeMerge(String existing, String verdict) throws Exception {
        Method m = NotificationService.class.getDeclaredMethod(
                "mergeResponse", String.class, String.class, java.time.Instant.class);
        m.setAccessible(true);
        return (String) m.invoke(service, existing, verdict, java.time.Instant.now());
    }

    private String invokeExtract(String payload) throws Exception {
        Method m = NotificationService.class.getDeclaredMethod(
                "extractResponse", String.class);
        m.setAccessible(true);
        return (String) m.invoke(service, payload);
    }
}