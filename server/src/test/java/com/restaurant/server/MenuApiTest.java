package com.restaurant.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.server.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MenuApiTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    /** V2.3 / V18 — same shared-state protection as AuthIntegrationTest: the
     *  rate limiter is a process singleton and must be cleared between
     *  tests, otherwise an earlier {@code rateLimitKicksInAfter5…} call
     *  locks the admin out of these tests. */
    @Autowired LoginRateLimiter rateLimiter;

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.clearAll();
    }

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private String login(String user, String pass) {
        String body = client().post().uri("/api/auth/login")
                .header("Content-Type", "application/json")
                .body("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                .retrieve().body(String.class);
        try { return mapper.readTree(body).path("data").path("token").asText(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void categoriesReturnsActiveOnly() {
        String token = login("admin", "admin123");
        String body = client().get().uri("/api/categories?lang=vi")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);
        assertNotNull(body);
        try {
            JsonNode arr = mapper.readTree(body).path("data");
            assertTrue(arr.isArray());
            assertTrue(arr.size() >= 5, "Should return at least 5 active categories");
            for (JsonNode c : arr) {
                assertEquals("ACTIVE", c.path("status").asText(), "Only ACTIVE categories returned");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void foodListingExcludesHidden() {
        String token = login("admin", "admin123");
        String body = client().get().uri("/api/foods?lang=vi&size=100")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);
        try {
            JsonNode arr = mapper.readTree(body).path("data").path("items");
            assertTrue(arr.isArray());
            for (JsonNode f : arr) {
                String status = f.path("status").asText();
                assertTrue(!"HIDDEN".equals(status), "STAFF must not see HIDDEN foods");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void featuredReturnsAvailableOnly() {
        String token = login("admin", "admin123");
        String body = client().get().uri("/api/foods/featured?limit=10&lang=vi")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);
        try {
            JsonNode arr = mapper.readTree(body).path("data");
            for (JsonNode f : arr) {
                assertEquals("AVAILABLE", f.path("status").asText());
                assertTrue(f.path("featured").asBoolean(), "featured must be true");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void languageSwitchReturnsDifferentText() {
        String token = login("admin", "admin123");
        String vi = client().get().uri("/api/foods/1?lang=vi")
                .header("Authorization", "Bearer " + token).retrieve().body(String.class);
        String ko = client().get().uri("/api/foods/1?lang=ko")
                .header("Authorization", "Bearer " + token).retrieve().body(String.class);
        try {
            String viName = mapper.readTree(vi).path("data").path("name").asText();
            String koName = mapper.readTree(ko).path("data").path("name").asText();
            assertNotNull(viName);
            assertNotNull(koName);
            // They should differ (the seed has distinct vi + ko names)
            assertTrue(!viName.equals(koName), "VI and KO names should differ");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void fallbackToViWhenKoMissing() {
        // The seed has every food with both translations; ask for a language
        // the server doesn't support (en). The server falls back to vi via
        // LocaleConfig.HeaderOrQueryLocaleResolver. The data response echoes
        // lang="vi" because that's what was actually rendered; the client
        // doesn't need a fallback marker for the language field because
        // there's no further-down fallback to disclose.
        String token = login("admin", "admin123");
        String body = client().get().uri("/api/foods/1?lang=en")
                .header("Authorization", "Bearer " + token).retrieve().body(String.class);
        try {
            JsonNode data = mapper.readTree(body).path("data");
            // The response always carries a name (either vi or ko); we don't
            // assert what it is because that's seed-data-dependent.
            assertTrue(data.path("name").asText().length() > 0, "name must not be empty");
            // Server resolves unsupported lang back to vi (the FALLBACK_LANG).
            assertEquals("vi", data.path("lang").asText(), "Unsupported lang falls back to vi");
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}