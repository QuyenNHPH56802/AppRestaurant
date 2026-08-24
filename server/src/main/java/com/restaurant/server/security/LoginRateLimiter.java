package com.restaurant.server.security;

import com.restaurant.server.config.RestaurantProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter for /api/auth/login. Per username+IP bucket.
 * Suitable for a single-node LAN server; a clustered deployment would swap this for Redis.
 */
@Component
public class LoginRateLimiter {

    private final RestaurantProperties props;
    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public LoginRateLimiter(RestaurantProperties props) {
        this.props = props;
    }

    public void recordFailed(String key) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(props.getRateLimit().getLogin().getWindowMinutes()));
        Deque<Instant> q = buckets.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            q.offerLast(Instant.now());
        }
    }

    public boolean isAllowed(String key) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(props.getRateLimit().getLogin().getWindowMinutes()));
        Deque<Instant> q = buckets.get(key);
        if (q == null) return true;
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            return q.size() < props.getRateLimit().getLogin().getAttempts();
        }
    }

    public void clear(String key) {
        buckets.remove(key);
    }

    /**
     * Test-only: drop every bucket. We expose this so {@code @BeforeEach}
     * can reset state between tests that share the singleton bean.
     * Production code must never call this; the buckets are per
     * username+IP and self-pruning.
     */
    public void clearAll() {
        buckets.clear();
    }
}