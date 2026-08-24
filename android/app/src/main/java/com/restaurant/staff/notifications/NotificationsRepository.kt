package com.restaurant.staff.notifications

import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.storage.LocaleStore
import com.restaurant.staff.storage.SessionStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2.3 / V18 — In-app notification feed reader + writer.
 *
 * Wraps the Retrofit [NotificationsApi] and adds the two cross-cutting
 * concerns that would otherwise leak into every caller:
 *   1. JWT injection — we read the current token from [SessionStore] for
 *      every call. We never store the token on the repo because that would
 *      outlive a logout.
 *   2. Locale selection — {@code ?lang=vi} or {@code ?lang=ko}, driven by
 *      the [LocaleStore] preference so the feed always shows in the user's
 *      currently chosen language.
 *
 * The repo is `open` so JVM unit tests can override its `suspend fun`s
 * without dragging in Robolectric (which would be needed to instantiate
 * the Context-bound stores). The overrides never call super, so the
 * store fields stay uninitialized safely.
 */
@Singleton
open class NotificationsRepository @Inject constructor(
    private val api: ApiClientProvider,
    private val session: SessionStore,
    private val locale: LocaleStore
) {
    open suspend fun list(page: Int = 0, size: Int = 20): NotificationListResponseDto? {
        val jwt = session.currentToken() ?: return null
        val lang = locale.language.first().ifBlank { "vi" }
        val env = api.notificationsApi().list("Bearer $jwt", page, size, lang)
        return if (env.success == true) env.data else null
    }

    open suspend fun unreadCount(): Long {
        val jwt = session.currentToken() ?: return 0
        val env = api.notificationsApi().unreadCount("Bearer $jwt")
        return env.data?.count ?: 0
    }

    open suspend fun markRead(id: Long): Boolean {
        val jwt = session.currentToken() ?: return false
        val env = api.notificationsApi().markRead("Bearer $jwt", id)
        return env.success == true
    }

    open suspend fun markAllRead(): Int {
        val jwt = session.currentToken() ?: return 0
        val env = api.notificationsApi().markAllRead("Bearer $jwt")
        @Suppress("UNCHECKED_CAST")
        return ((env.data?.get("markedRead") as? Number)?.toInt() ?: 0)
    }

    open suspend fun events(id: Long): List<NotificationEventViewDto> {
        val jwt = session.currentToken() ?: return emptyList()
        val env = api.notificationsApi().events("Bearer $jwt", id)
        return env.data ?: emptyList()
    }

    /**
     * V2.3 / V18 — Accept/decline a notification. Returns the canonical
     * verdict echoed by the server, or null on auth/IO failure.
     *
     * {@code verdict} must be exactly "ACCEPTED" or "DECLINED" — the server
     * regex-validates this so we don't need to re-check on the client, but
     * we still restrict to those two literals here so a UI bug can't send
     * something nonsensical.
     */
    open suspend fun respond(notificationId: Long, verdict: String): String? {
        if (verdict != "ACCEPTED" && verdict != "DECLINED") return null
        val jwt = session.currentToken() ?: return null
        val env = api.notificationsApi().respond(
            "Bearer $jwt", notificationId, RespondRequest(verdict)
        )
        return if (env.success == true) env.data?.verdict else null
    }
}