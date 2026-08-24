package com.restaurant.staff.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * V2.3 / V18 — Phase F — ViewModel logic tests.
 *
 * The repo's surface is small and well-defined, so we hand-roll a
 * [FakeNotificationsDataSource] rather than drag mockito-kotlin into the
 * unit-test classpath. Each test sets up the fake, exercises one ViewModel
 * method, and asserts on the StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(ds: FakeNotificationsDataSource): NotificationsViewModel =
        NotificationsViewModel(ds)

    @Test
    fun refreshPopulatesItemsAndCount() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(
                    NotificationViewDto(id = 1, type = "SHIFT_ASSIGNED", title = "Ca sáng", readAt = null),
                    NotificationViewDto(id = 2, type = "ZONE_CHANGED", title = "Đổi khu vực", readAt = "2026-08-24T01:00:00Z"),
                ),
                page = 0, size = 20, total = 2, totalPages = 1
            )
            unreadResponse = 1L
        }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(2, s.items.size)
        assertEquals(1L, s.unreadCount)
        assertFalse(s.loading)
        assertTrue(s.endReached)
        assertNull(s.error)
    }

    @Test
    fun unreadFilterHidesReadItems() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(
                    NotificationViewDto(id = 1, type = "SHIFT_ASSIGNED", title = "Ca sáng", readAt = null),
                    NotificationViewDto(id = 2, type = "ZONE_CHANGED", title = "Đổi khu vực", readAt = "2026-08-24T01:00:00Z"),
                ),
                page = 0, size = 20, total = 2, totalPages = 1
            )
        }
        val vm = makeViewModel(ds)
        vm.setFilter(NotificationsViewModel.Filter.UNREAD_ONLY)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(NotificationsViewModel.Filter.UNREAD_ONLY, s.filter)
        assertEquals(1, s.items.size)
        assertEquals(1L, s.items.first().id)
    }

    @Test
    fun markReadSetsTimestampAndDecrementsCount() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(NotificationViewDto(id = 1, type = "X", title = "T", readAt = null)),
                page = 0, size = 20, total = 1, totalPages = 1
            )
            unreadResponse = 1L
            markReadResult = true
        }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1L, vm.state.value.unreadCount)

        vm.markRead(1L)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(0L, s.unreadCount)
        assertEquals("now", s.items.first().readAt)
    }

    @Test
    fun markReadHidesItemUnderUnreadFilter() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(
                    NotificationViewDto(id = 1, type = "X", title = "T", readAt = null),
                    NotificationViewDto(id = 2, type = "X", title = "T2", readAt = null)
                ),
                page = 0, size = 20, total = 2, totalPages = 1
            )
            unreadResponse = 2L
            markReadResult = true
        }
        val vm = makeViewModel(ds)
        vm.setFilter(NotificationsViewModel.Filter.UNREAD_ONLY)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.items.size)

        vm.markRead(1L)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(1, s.items.size)
        assertEquals(2L, s.items.first().id)
    }

    @Test
    fun markAllReadHidesAllUnderUnreadFilter() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(
                    NotificationViewDto(id = 1, type = "X", title = "T", readAt = null),
                    NotificationViewDto(id = 2, type = "X", title = "T2", readAt = null)
                ),
                page = 0, size = 20, total = 2, totalPages = 1
            )
            unreadResponse = 2L
            markAllReadResult = 2
        }
        val vm = makeViewModel(ds)
        vm.setFilter(NotificationsViewModel.Filter.UNREAD_ONLY)
        advanceUntilIdle()

        vm.markAllRead()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(0L, s.unreadCount)
        assertEquals(0, s.items.size)
    }

    @Test
    fun respondUpdatesResponsesMapAndClearsResponding() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            listResponse = NotificationListResponseDto(
                items = listOf(NotificationViewDto(id = 1, type = "SHIFT_ASSIGNED", title = "T", readAt = null)),
                page = 0, size = 20, total = 1, totalPages = 1
            )
            respondResult = "ACCEPTED"
        }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()

        vm.respond(1L, "ACCEPTED")
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals("ACCEPTED", s.responses[1L])
        assertFalse(s.responding.contains(1L))
    }

    @Test
    fun respondRejectsInvalidVerdict() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource()
        val vm = makeViewModel(ds)
        vm.respond(1L, "MAYBE")
        assertTrue(vm.state.value.responses.isEmpty())
    }

    @Test
    fun loadMoreAppendsItemsAndRespectsEndReached() = runTest(dispatcher) {
        val page1 = NotificationListResponseDto(
            items = listOf(NotificationViewDto(id = 1, type = "X", title = "T1")),
            page = 0, size = 20, total = 3, totalPages = 2
        )
        val page2 = NotificationListResponseDto(
            items = listOf(NotificationViewDto(id = 2, type = "X", title = "T2")),
            page = 1, size = 20, total = 3, totalPages = 2
        )
        val ds = FakeNotificationsDataSource().apply {
            listResponses.add(page1)
            listResponses.add(page2)
        }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.items.size)
        assertFalse(vm.state.value.endReached)

        vm.loadMore()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(2, s.items.size)
        assertTrue(s.endReached)
    }

    @Test
    fun refreshFailureStampsError() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply { throwOnList = true }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.loading)
        assertNotNull(s.error)
    }

    @Test
    fun openEventsSurfacesAuditTrail() = runTest(dispatcher) {
        val events = listOf(
            NotificationEventViewDto(id = 10, notificationId = 1, channel = "PUSH", status = "SENT", attempts = 1),
            NotificationEventViewDto(id = 11, notificationId = 1, channel = "PUSH", status = "RETRYABLE", attempts = 2, errorCode = "UNAVAILABLE")
        )
        val ds = FakeNotificationsDataSource().apply { eventsResult = events }
        val vm = makeViewModel(ds)
        vm.openEvents(1L)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(1L, s.eventsForNotificationId)
        assertEquals(2, s.events.size)
        assertFalse(s.loadingEvents)
    }

    @Test
    fun closeEventsClearsDialogState() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply {
            eventsResult = listOf(NotificationEventViewDto(id = 1, notificationId = 1))
        }
        val vm = makeViewModel(ds)
        vm.openEvents(1L)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.events.size)

        vm.closeEvents()
        assertEquals(0, vm.state.value.events.size)
        assertNull(vm.state.value.eventsForNotificationId)
    }

    @Test
    fun clearErrorResetsErrorStamp() = runTest(dispatcher) {
        val ds = FakeNotificationsDataSource().apply { throwOnList = true }
        val vm = makeViewModel(ds)
        vm.refresh()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.clearError()
        assertNull(vm.state.value.error)
    }
}

/**
 * Phase F — Hand-rolled fake [NotificationsDataSource] for unit tests.
 *
 * The fake stores its state directly in the test thread; the dispatcher
 * uses [StandardTestDispatcher] so the launch{} inside the VM runs on the
 * same controlled thread.
 */
class FakeNotificationsDataSource : NotificationsDataSource {
    var listResponse: NotificationListResponseDto? = null
    val listResponses: MutableList<NotificationListResponseDto> = mutableListOf()
    var unreadResponse: Long? = null
    var markReadResult: Boolean = false
    var markAllReadResult: Int = 0
    var respondResult: String? = null
    var eventsResult: List<NotificationEventViewDto> = emptyList()
    var throwOnList: Boolean = false

    override suspend fun list(page: Int, size: Int): NotificationListResponseDto? {
        if (throwOnList) throw RuntimeException("test_network_error")
        return if (listResponses.isNotEmpty()) listResponses.removeAt(0) else listResponse
    }

    override suspend fun unreadCount(): Long = unreadResponse ?: 0L

    override suspend fun markRead(id: Long): Boolean = markReadResult

    override suspend fun markAllRead(): Int = markAllReadResult

    override suspend fun events(id: Long): List<NotificationEventViewDto> = eventsResult

    override suspend fun respond(notificationId: Long, verdict: String): String? = respondResult
}