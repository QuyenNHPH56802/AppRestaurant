package com.restaurant.server.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PHASE 2 smoke test for entity class loading. Ensures the JPA entity hierarchy
 * compiles and the default constructors are present.
 */
class EntitySmokeTest {

    @Test
    void userEntityIsLoadable() {
        User u = new User();
        u.setUsername("test");
        u.setPasswordHash("x");
        u.setFullName("Test");
        u.setRole(User.Role.STAFF);
        assertNotNull(u);
    }

    @Test
    void categoryEntityIsLoadable() {
        Category c = new Category();
        c.setSortOrder(0);
        assertNotNull(c);
    }

    @Test
    void foodEntityIsLoadable() {
        Food f = new Food();
        f.setSortOrder(0);
        f.setFeatured(false);
        assertNotNull(f);
    }

    @Test
    void storeSettingsEntityIsLoadable() {
        StoreSettings s = new StoreSettings();
        s.setId(1L);
        assertNotNull(s);
    }
}