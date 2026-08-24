package com.restaurant.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ContextLoadTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoadsAndMigrationsApplied() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(2, count, "Seed users from V2 should be present");

        Integer foodCount = jdbc.queryForObject("SELECT COUNT(*) FROM foods", Integer.class);
        assertEquals(25, foodCount);

        Integer catCount = jdbc.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        assertEquals(5, catCount);

        Integer storeCount = jdbc.queryForObject("SELECT COUNT(*) FROM store_settings", Integer.class);
        assertEquals(1, storeCount);

        // FK = ON
        Integer fk = jdbc.queryForObject("PRAGMA foreign_keys", Integer.class);
        assertEquals(1, fk, "foreign_keys must be ON");
    }

    @Test
    void flywayHistoryIsRecorded() {
        Integer flywayApplied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertTrue(flywayApplied != null && flywayApplied >= 2, "At least V1 and V2 migrations applied");
    }
}