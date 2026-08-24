package com.restaurant.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase G - Smoke test for the admin SPA static resources.
 *
 * <p>Verifies that the admin shell, JS bundle and CSS are still served at the
 * documented URLs after the V2.2/V2.3 menu additions. We do NOT parse the
 * JS for syntax - that's the browser's job; we just confirm the bytes are
 * served and contain the markers that should exist (e.g. Phase G hooks).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminSpaSmokeTest {

    @LocalServerPort int port;

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void adminIndexIsServed() {
        ResponseEntity<String> resp = client().get()
                .uri("/admin/app/index.html")
                .retrieve()
                .toEntity(String.class);
        assertEquals(200, resp.getStatusCode().value());
        String body = resp.getBody();
        assertNotNull(body);
        // V2.2/V2.3 nav items must be present. We look for data-view attributes
        // rather than Vietnamese labels to avoid mojibake in the test source.
        assertTrue(body.contains("data-view=\"shifts\""),        "Nav should expose shifts view");
        assertTrue(body.contains("data-view=\"zones\""),         "Nav should expose zones view");
        assertTrue(body.contains("data-view=\"checklists\""),    "Nav should expose checklists view");
        assertTrue(body.contains("data-view=\"checkins\""),      "Nav should expose checkins view");
        assertTrue(body.contains("data-view=\"activity\""),      "Nav should expose activity view");
        assertTrue(body.contains("data-view=\"notifications\""), "Nav should expose notifications view");
        assertTrue(body.contains("data-view=\"devices\""),       "Nav should expose devices view");
    }

    @Test
    void adminJsBundleIsServed() {
        ResponseEntity<String> resp = client().get()
                .uri("/admin/app/app.js")
                .retrieve()
                .toEntity(String.class);
        assertEquals(200, resp.getStatusCode().value());
        String body = resp.getBody();
        assertNotNull(body);
        // Make sure each Phase G render function is present
        assertTrue(body.contains("renderShifts"),        "JS should define renderShifts");
        assertTrue(body.contains("renderShiftAssignments"), "JS should define renderShiftAssignments");
        assertTrue(body.contains("renderZones"),         "JS should define renderZones");
        assertTrue(body.contains("renderChecklists"),    "JS should define renderChecklists");
        assertTrue(body.contains("renderCheckIns"),      "JS should define renderCheckIns");
        assertTrue(body.contains("renderActivity"),      "JS should define renderActivity");
        assertTrue(body.contains("renderNotifications"), "JS should define renderNotifications");
        assertTrue(body.contains("renderDevices"),       "JS should define renderDevices");
    }

    @Test
    void adminCssBundleIsServed() {
        ResponseEntity<String> resp = client().get()
                .uri("/admin/app/app.css")
                .retrieve()
                .toEntity(String.class);
        assertEquals(200, resp.getStatusCode().value());
        String body = resp.getBody();
        assertNotNull(body);
        // New V2.2/V2.3 classes must be present
        assertTrue(body.contains(".tabs"),       "CSS should define .tabs");
        assertTrue(body.contains(".tab"),        "CSS should define .tab");
        assertTrue(body.contains(".grid-2"),     "CSS should define .grid-2");
        assertTrue(body.contains(".zone-card"),  "CSS should define .zone-card");
        assertTrue(body.contains(".color-dot"),  "CSS should define .color-dot");
        assertTrue(body.contains(".task-row"),   "CSS should define .task-row");
        assertTrue(body.contains(".json-cell"),  "CSS should define .json-cell");
    }
}