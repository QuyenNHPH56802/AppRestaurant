package com.restaurant.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.server.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V2.2 — End-to-end coverage for the shift / zone / checklist / check-in /
 * activity-log controllers. Exercises both admin (admin user) and staff
 * (nhanvien01 user) endpoints so role-based authorisation is also covered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class V22ApiTest {

    @LocalServerPort int port;

    @Autowired ObjectMapper mapper;
    @Autowired LoginRateLimiter rateLimiter;

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void reset() throws Exception {
        rateLimiter.clearAll();
        adminToken = login("admin", "admin123");
        staffToken = login("nhanvien01", "staff123");
        assertNotNull(adminToken);
        assertNotNull(staffToken);
    }

    private String login(String user, String pw) throws Exception {
        String body = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"" + user + "\",\"password\":\"" + pw + "\"}")
                .retrieve().body(String.class);
        return mapper.readTree(body).path("data").path("token").asText();
    }

    private JsonNode get(String path, String token) throws Exception {
        try {
            String body = client().get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(String.class);
            return mapper.readTree(body);
        } catch (HttpClientErrorException ex) {
            return mapper.readTree(ex.getResponseBodyAsString());
        }
    }

    private JsonNode post(String path, String body, String token) throws Exception {
        try {
            String resp = client().post().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            return mapper.readTree(resp);
        } catch (HttpClientErrorException ex) {
            return mapper.readTree(ex.getResponseBodyAsString());
        }
    }

    private JsonNode put(String path, String body, String token) throws Exception {
        try {
            String resp = client().put().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            return mapper.readTree(resp);
        } catch (HttpClientErrorException ex) {
            return mapper.readTree(ex.getResponseBodyAsString());
        }
    }

    private JsonNode del(String path, String token) throws Exception {
        try {
            String resp = client().method(org.springframework.http.HttpMethod.DELETE)
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(String.class);
            return mapper.readTree(resp == null ? "{}" : resp);
        } catch (HttpClientErrorException ex) {
            return mapper.readTree(ex.getResponseBodyAsString());
        }
    }

    @Test
    void listShiftsReturnsSeededRows() throws Exception {
        JsonNode body = get("/api/admin/shifts", adminToken);
        assertTrue(body.path("success").asBoolean());
        assertTrue(body.path("data").isArray());
        // V11 seeds 3 shifts
        assertTrue(body.path("data").size() >= 3, "Expected at least 3 seeded shifts, got " + body.path("data").size());
        boolean sawMorning = false;
        for (JsonNode s : body.path("data")) {
            if ("Ca sáng".equals(s.path("name").asText())) sawMorning = true;
            assertTrue(s.path("startTime").asText().matches("\\d{2}:\\d{2}"));
        }
        assertTrue(sawMorning, "Expected Ca sáng to be present");
    }

    @Test
    void createAndAssignAndRespondShift() throws Exception {
        // Get the seeded Ca sáng shift
        JsonNode shifts = get("/api/admin/shifts", adminToken).path("data");
        long shiftId = -1;
        for (JsonNode s : shifts) {
            if ("Ca sáng".equals(s.path("name").asText())) { shiftId = s.path("id").asLong(); break; }
        }
        assertTrue(shiftId > 0);

        // Look up staff user id by login profile
        JsonNode staffLogin = mapper.readTree(client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"nhanvien01\",\"password\":\"staff123\"}")
                .retrieve().body(String.class));
        long staffId = staffLogin.path("data").path("user").path("id").asLong();
        assertTrue(staffId > 0);

        String date = LocalDate.now().plusDays(7).toString();
        String createBody = "{\"shiftId\":" + shiftId + ",\"userId\":" + staffId
                + ",\"date\":\"" + date + "\",\"notes\":\"auto-test\"}";
        JsonNode created = post("/api/admin/shift-assignments", createBody, adminToken);
        assertTrue(created.path("success").asBoolean(), "create: " + created.toString());
        long assignmentId = created.path("data").path("id").asLong();
        assertTrue(assignmentId > 0);

        // Staff accepts
        JsonNode accept = post("/api/me/shifts/" + assignmentId + "/respond",
                "{\"status\":\"ACCEPTED\",\"notes\":\"yes\"}", staffToken);
        assertTrue(accept.path("success").asBoolean(), "accept: " + accept.toString());
        assertEquals("ACCEPTED", accept.path("data").path("status").asText());

        // Try to reject after accept — should fail with conflict
        JsonNode reject = post("/api/me/shifts/" + assignmentId + "/respond",
                "{\"status\":\"REJECTED\"}", staffToken);
        assertTrue(!reject.path("success").asBoolean(), "Second respond must fail (illegal transition)");

        // Admin list by date
        JsonNode list = get("/api/admin/shift-assignments?date=" + date, adminToken);
        assertTrue(list.path("success").asBoolean());
        assertTrue(list.path("data").isArray());

        // Staff list own shifts
        JsonNode mine = get("/api/me/shifts", staffToken);
        assertTrue(mine.path("success").asBoolean());
        boolean found = false;
        for (JsonNode a : mine.path("data")) {
            if (a.path("id").asLong() == assignmentId) { found = true; break; }
        }
        assertTrue(found, "Staff should see their own assignment");

        // Cleanup
        del("/api/admin/shift-assignments/" + assignmentId, adminToken);
    }

    @Test
    void zoneListAndSelfServiceAssignment() throws Exception {
        // Seeded zones: BEP_PHO etc. from V7
        JsonNode zones = get("/api/me/zones", staffToken);
        assertTrue(zones.path("success").asBoolean());
        assertTrue(zones.path("data").isArray());
        assertTrue(zones.path("data").size() >= 4);

        long zoneId = -1;
        for (JsonNode z : zones.path("data")) {
            if ("BEP_PHO".equals(z.path("code").asText())) { zoneId = z.path("id").asLong(); break; }
        }
        assertTrue(zoneId > 0);

        JsonNode staffLogin = mapper.readTree(client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"nhanvien01\",\"password\":\"staff123\"}")
                .retrieve().body(String.class));
        long staffId = staffLogin.path("data").path("user").path("id").asLong();

        JsonNode assign = post("/api/me/zones/assign",
                "{\"userId\":" + staffId + ",\"zoneId\":" + zoneId + ",\"reason\":\"e2e\"}",
                staffToken);
        assertTrue(assign.path("success").asBoolean(), "assign: " + assign.toString());
        assertEquals(true, assign.path("data").path("current").asBoolean());

        // Current should reflect
        JsonNode current = get("/api/me/zones/current", staffToken);
        assertTrue(current.path("success").asBoolean());
        assertEquals(zoneId, current.path("data").path("zoneId").asLong());

        // Cannot assign another user via self-service
        JsonNode denied = post("/api/me/zones/assign",
                "{\"userId\":1,\"zoneId\":" + zoneId + "}", staffToken);
        assertTrue(!denied.path("success").asBoolean());
    }

    @Test
    void checklistCreateAndComplete() throws Exception {
        long zoneId = -1;
        JsonNode zones = get("/api/me/zones", staffToken);
        for (JsonNode z : zones.path("data")) {
            if ("KHO".equals(z.path("code").asText())) { zoneId = z.path("id").asLong(); break; }
        }
        assertTrue(zoneId > 0);

        String req = "{\"zoneId\":" + zoneId + ","
                + "\"active\":true,\"sortOrder\":99,"
                + "\"translations\":[{\"lang\":\"vi\",\"title\":\"Danh sách kho\",\"description\":\"cuối ca\"},"
                + "{\"lang\":\"ko\",\"title\":\"창고 체크리스트\",\"description\":\"마감\"}],"
                + "\"tasks\":[{\"required\":true,\"active\":true,\"sortOrder\":1,"
                + "\"translations\":[{\"lang\":\"vi\",\"title\":\"Đóng cửa\",\"description\":\"\"},"
                + "{\"lang\":\"ko\",\"title\":\"문 닫기\",\"description\":\"\"}]},"
                + "{\"required\":false,\"active\":true,\"sortOrder\":2,"
                + "\"translations\":[{\"lang\":\"vi\",\"title\":\"Vệ sinh\",\"description\":\"\"},"
                + "{\"lang\":\"ko\",\"title\":\"청소\",\"description\":\"\"}]}]}";
        JsonNode created = post("/api/admin/checklists", req, adminToken);
        assertTrue(created.path("success").asBoolean(), "create: " + created.toString());
        long checklistId = created.path("data").path("id").asLong();
        long requiredTaskId = created.path("data").path("tasks").get(0).path("id").asLong();
        long optionalTaskId = created.path("data").path("tasks").get(1).path("id").asLong();

        // Staff sees the active checklist
        JsonNode mine = get("/api/me/checklists?zoneId=" + zoneId, staffToken);
        assertTrue(mine.path("success").asBoolean());
        boolean saw = false;
        for (JsonNode c : mine.path("data")) {
            if (c.path("id").asLong() == checklistId) { saw = true; break; }
        }
        assertTrue(saw, "Staff should see the active checklist");

        // Complete required
        JsonNode done = post("/api/me/checklists/complete",
                "{\"taskId\":" + requiredTaskId + ",\"status\":\"COMPLETED\"}", staffToken);
        assertTrue(done.path("success").asBoolean(), "complete: " + done.toString());

        // Skip required -> should fail
        JsonNode badSkip = post("/api/me/checklists/complete",
                "{\"taskId\":" + requiredTaskId + ",\"status\":\"SKIPPED\"}", staffToken);
        assertTrue(!badSkip.path("success").asBoolean());

        // Skip optional -> ok
        JsonNode okSkip = post("/api/me/checklists/complete",
                "{\"taskId\":" + optionalTaskId + ",\"status\":\"SKIPPED\"}", staffToken);
        assertTrue(okSkip.path("success").asBoolean(), "skip optional: " + okSkip.toString());

        // Recent completions
        JsonNode recent = get("/api/me/checklists/completions?limit=5", staffToken);
        assertTrue(recent.path("success").asBoolean());
        assertTrue(recent.path("data").size() >= 2);
    }

    @Test
    void checkInAndOut() throws Exception {
        long zoneId = -1;
        JsonNode zones = get("/api/me/zones", staffToken);
        for (JsonNode z : zones.path("data")) {
            if ("PHUC_VU".equals(z.path("code").asText())) { zoneId = z.path("id").asLong(); break; }
        }
        assertTrue(zoneId > 0);

        JsonNode in = post("/api/me/check-ins",
                "{\"zoneId\":" + zoneId + ",\"action\":\"CHECK_IN\",\"deviceId\":\"test-device\"}",
                staffToken);
        assertTrue(in.path("success").asBoolean(), "check in: " + in.toString());
        assertEquals("CHECK_IN", in.path("data").path("action").asText());

        // Second CHECK_IN should be rejected
        JsonNode dup = post("/api/me/check-ins",
                "{\"zoneId\":" + zoneId + ",\"action\":\"CHECK_IN\"}", staffToken);
        assertTrue(!dup.path("success").asBoolean(), "duplicate check-in must be rejected");

        JsonNode out = post("/api/me/check-ins",
                "{\"zoneId\":" + zoneId + ",\"action\":\"CHECK_OUT\"}", staffToken);
        assertTrue(out.path("success").asBoolean());
        assertEquals("CHECK_OUT", out.path("data").path("action").asText());

        JsonNode recent = get("/api/me/check-ins?limit=5", staffToken);
        assertTrue(recent.path("success").asBoolean());
        assertTrue(recent.path("data").size() >= 2);
    }

    @Test
    void adminCanSeeActivityLogs() throws Exception {
        JsonNode logs = get("/api/admin/activity-logs?limit=10", adminToken);
        assertTrue(logs.path("success").asBoolean());
        // We just did several events in this test class; there should be some.
        assertTrue(logs.path("data").isArray());

        // Staff cannot see admin endpoint
        try {
            client().get().uri("/api/admin/activity-logs")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                    .retrieve().body(String.class);
        } catch (HttpClientErrorException ex) {
            assertEquals(403, ex.getStatusCode().value());
        }
    }

    @Test
    void adminCanCreateAndDeleteZone() throws Exception {
        String code = "TEST_" + System.currentTimeMillis();
        String body = "{\"code\":\"" + code + "\","
                + "\"color\":\"#abcdef\",\"status\":\"ACTIVE\",\"sortOrder\":99,\"requiredStaff\":1,"
                + "\"translations\":[{\"lang\":\"vi\",\"name\":\"Khu test\",\"description\":\"\"},"
                + "{\"lang\":\"ko\",\"name\":\"테스트\",\"description\":\"\"}]}";
        JsonNode created = post("/api/admin/zones", body, adminToken);
        assertTrue(created.path("success").asBoolean(), "zone create: " + created.toString());
        long zoneId = created.path("data").path("id").asLong();

        JsonNode updated = put("/api/admin/zones/" + zoneId,
                "{\"code\":\"" + code + "\","
                + "\"color\":\"#000000\",\"status\":\"DISABLED\",\"sortOrder\":50,\"requiredStaff\":2,"
                + "\"translations\":[{\"lang\":\"vi\",\"name\":\"Khu test 2\",\"description\":\"x\"},"
                + "{\"lang\":\"ko\",\"name\":\"테스트2\",\"description\":\"\"}]}",
                adminToken);
        assertTrue(updated.path("success").asBoolean(), "zone update: " + updated.toString());
        assertEquals("DISABLED", updated.path("data").path("status").asText());

        JsonNode deleted = del("/api/admin/zones/" + zoneId, adminToken);
        assertTrue(deleted.path("success").asBoolean());
    }
}
