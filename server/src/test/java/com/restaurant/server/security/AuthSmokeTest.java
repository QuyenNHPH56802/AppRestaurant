package com.restaurant.server.security;

import com.restaurant.server.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PHASE 3 smoke test. Real JWT auth + service layer tests start in PHASE 10.
 */
class AuthSmokeTest {

    @Test
    void roleEnumIsLoadable() {
        assertNotNull(User.Role.ADMIN);
        assertNotNull(User.Role.STAFF);
        assertNotNull(User.Status.ACTIVE);
        assertNotNull(User.Status.DISABLED);
    }
}