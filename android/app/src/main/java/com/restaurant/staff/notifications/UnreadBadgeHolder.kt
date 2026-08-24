package com.restaurant.staff.notifications

import com.restaurant.staff.storage.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2.3 / V18 — Holds the current unread-notification count and refreshes it
 * on a fixed interval while the user is signed in.
 *
 * Scope:
 *   - Process-singleton, lives as long as the app.
 *   - Polling runs only while {@link #start} has been called and is cancelled
 *     by {@link #stop} (called on logout). Battery: we re-fetch every 30s
 *     instead of every few seconds; the system can throttle the worker if
 *     the app is backgrounded.
 *   - State is exposed as a [StateFlow] so Compose can collect it cheaply.
 *
 * Why not WorkManager:
 *   - WorkManager would force us to define a periodic worker AND a way for
 *     the badge UI to receive updates, which means a one-way event channel.
 *     For a count that drives a single nav-bar item, the simpler "while the
 *     app is open, refresh every 30s" is enough. WorkManager would shine if
 *     we wanted the badge to show on the home screen launcher icon or a
 *     widget — neither is in scope for v2.3.
 */
@Singleton
class UnreadBadgeHolder @Inject constructor(
    private val repo: NotificationsRepository,
    private val session: SessionStore
) {
    /** The displayed count. 0 means "no badge". */
    private val _count = MutableStateFlow(0L)
    val count: StateFlow<Long> = _count.asStateFlow()

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Begin polling. Idempotent — calling twice keeps the existing job. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                refresh()
                delay(POLL_MS)
            }
        }
    }

    /** Cancel the loop. Used on logout so the badge doesn't update behind the
     *  login screen. */
    fun stop() {
        job?.cancel()
        job = null
        _count.value = 0L
    }

    /** Force a one-shot refresh, e.g. after markAllRead returns. */
    suspend fun refresh() {
        // If the user has logged out, the repo returns 0 — no work needed
        // beyond what's already there. We still call it because a transition
        // from "signed in" to "signed out" must drop the badge to 0 quickly.
        val n = runCatching { repo.unreadCount() }.getOrDefault(0L)
        _count.value = n
    }

    /** Shutdown for testing; mirrors [stop] but also tears down the scope. */
    fun shutdownForTest() {
        scope.cancel()
    }

    private companion object {
        const val POLL_MS = 30_000L
    }
}