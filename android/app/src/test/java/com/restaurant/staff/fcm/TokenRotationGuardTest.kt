package com.restaurant.staff.fcm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2.3 / V18 — Pure-function tests for the rotation guard.
 *
 * The guard decides whether an FCM token should be POSTed to the server.
 * It runs on every cold-start and after every onNewToken callback, so any
 * regression here would either spam the server with redundant POSTs or
 * silently fail to register a rotated token.
 */
class TokenRotationGuardTest {

    @Test
    fun postsWhenNoPreviousToken() {
        // First registration after install: no baseline.
        assertTrue(TokenRotationGuard.shouldPost(previous = null, current = "tok-A"))
    }

    @Test
    fun postsWhenTokenChanged() {
        // FCM rotated: previous="tok-A" → current="tok-B".
        assertTrue(TokenRotationGuard.shouldPost(previous = "tok-A", current = "tok-B"))
    }

    @Test
    fun skipsWhenTokenUnchanged() {
        // OS occasionally re-delivers the same token after a hot restart.
        // Don't spam the server.
        assertFalse(TokenRotationGuard.shouldPost(previous = "tok-A", current = "tok-A"))
    }

    @Test
    fun forcedAlwaysPosts() {
        // After a logout/login on the same device, the server row may have
        // been deactivated; force a POST so it flips back to active.
        assertTrue(TokenRotationGuard.shouldPost(previous = "tok-A", current = "tok-A", forced = true))
    }

    @Test
    fun emptyTokensAreDistinct() {
        // Defensive: an empty previous string is still distinct from a real one.
        assertTrue(TokenRotationGuard.shouldPost(previous = "", current = "tok-A"))
    }
}