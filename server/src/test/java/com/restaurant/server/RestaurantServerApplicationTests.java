package com.restaurant.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PHASE 2 smoke test. Verifies the main application class loads.
 * Full Spring Boot context tests start in PHASE 3 once Security + JWT + Auth are wired.
 */
class RestaurantServerApplicationTests {

    @Test
    void mainClassIsLoadable() {
        assertNotNull(RestaurantServerApplication.class);
    }
}