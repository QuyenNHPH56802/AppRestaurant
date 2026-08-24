package com.restaurant.staff.notifications

/**
 * V2.3 / V18 — Phase F — Pure data-source abstraction for the
 * notification feed.
 *
 * Hilt provides [NotificationsDataSourceImpl] (which delegates to
 * [NotificationsRepository]). Tests provide a hand-rolled fake so the
 * ViewModel can be unit-tested without Robolectric / Mockito.
 */
interface NotificationsDataSource {
    suspend fun list(page: Int, size: Int): NotificationListResponseDto?
    suspend fun unreadCount(): Long
    suspend fun markRead(id: Long): Boolean
    suspend fun markAllRead(): Int
    suspend fun events(id: Long): List<NotificationEventViewDto>
    suspend fun respond(notificationId: Long, verdict: String): String?
}