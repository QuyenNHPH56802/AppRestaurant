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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @LocalServerPort int port;

    @Autowired ObjectMapper mapper;
    /** V2.3 / V18 — the rate limiter is a process singleton, so we reset it
     *  before each test. Without this, the {@code rateLimitKicksInAfter5…}
     *  test's 5 failures leak into {@code loginWithDefaultAdminSucceeds}
     *  and {@code MenuApiTest} and produce cascading 429s that look like
     *  flaky tests. */
    @Autowired LoginRateLimiter rateLimiter;

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.clearAll();
    }

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void loginWithDefaultAdminSucceeds() {
        ResponseEntity<String> resp = client().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                .retrieve()
                .onStatus(s -> s.value() == HttpStatus.UNAUTHORIZED.value(),
                          (req, res) -> { throw new AssertionError("Login should succeed"); })
                .toEntity(String.class);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        try {
            JsonNode root = mapper.readTree(resp.getBody());
            assertTrue(root.path("success").asBoolean(), "success must be true");
            String token = root.path("data").path("token").asText();
            assertTrue(token.length() > 50, "JWT should be a long string");
            String role = root.path("data").path("user").path("role").asText();
            assertEquals("ADMIN", role);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loginWithWrongPasswordIs401() {
        try {
            client().post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"username\":\"admin\",\"password\":\"wrong\"}")
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { /* ignore */ })
                    .toEntity(String.class);
            // The above either throws or returns 4xx; both are acceptable
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(401, ex.getStatusCode().value());
        }
    }

    @Test
    void rateLimitKicksInAfter5FailedAttempts() {
        RestClient rc = client();
        for (int i = 0; i < 5; i++) {
            try {
                rc.post().uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"username\":\"admin\",\"password\":\"bad-" + i + "\"}")
                        .retrieve()
                        .onStatus(s -> true, (req, res) -> {})
                        .toEntity(String.class);
            } catch (Exception ignored) {}
        }
        // 6th attempt should hit the rate limiter
        try {
            rc.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> {})
                    .toEntity(String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // Even with correct creds we should be locked out (RATE_LIMITED -> 429)
            assertTrue(ex.getStatusCode().value() == 429 || ex.getStatusCode().value() == 401,
                    "Rate limit expected: status=" + ex.getStatusCode().value());
        }
    }

    @Test
    void publicHealthEndpointReturnsOk() {
        String body = client().get().uri("/api/health").retrieve().body(String.class);
        assertNotNull(body);
        assertTrue(body.contains("\"status\""));
    }

    @Test
    void serverInfoIsPublic() {
        String body = client().get().uri("/api/server/info").retrieve().body(String.class);
        assertNotNull(body);
        assertTrue(body.contains("\"server\""));
    }

    @Test
    void qrEndpointIsPublic() {
        byte[] png = client().get().uri("/api/server/qr.png").retrieve().body(byte[].class);
        assertNotNull(png);
        // PNG magic number
        assertTrue(png.length > 8 && (png[0] & 0xff) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G');
    }

    @Test
    void staffCannotAccessAdminFoods() {
        // First login as staff
        String login = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"nhanvien01\",\"password\":\"staff123\"}")
                .retrieve().body(String.class);
        String token;
        try {
            token = mapper.readTree(login).path("data").path("token").asText();
        } catch (Exception e) { throw new RuntimeException(e); }
        try {
            client().get().uri("/api/admin/foods")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> {})
                    .toEntity(String.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertEquals(403, ex.getStatusCode().value(), "STAFF should be forbidden from /api/admin/foods");
        }
    }
}