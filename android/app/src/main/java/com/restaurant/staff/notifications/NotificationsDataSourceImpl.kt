package com.restaurant.staff.notifications

import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2.3 / V18 — Phase F — Hilt-friendly adapter that delegates the
 * [NotificationsDataSource] interface to the existing [NotificationsRepository].
 *
 * Keeping the interface + adapter separate means unit tests can swap in a
 * hand-rolled fake without subclassing [NotificationsRepository] (whose
 * constructor needs Context-bound stores and is awkward to instantiate in
 * plain JVM tests).
 */
@Singleton
class NotificationsDataSourceImpl @Inject constructor(
    private val repo: NotificationsRepository
) : NotificationsDataSource {

    override suspend fun list(page: Int, size: Int): NotificationListResponseDto? =
        repo.list(page = page, size = size)

    override suspend fun unreadCount(): Long = repo.unreadCount()

    override suspend fun markRead(id: Long): Boolean = repo.markRead(id)

    override suspend fun markAllRead(): Int = repo.markAllRead()

    override suspend fun events(id: Long): List<NotificationEventViewDto> = repo.events(id)

    override suspend fun respond(notificationId: Long, verdict: String): String? =
        repo.respond(notificationId, verdict)
}