package com.restaurant.staff.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.3 / V18 — Phase F — State holder for the in-app notification feed.
 *
 * UI states surfaced:
 *   - {@code items}        — accumulated list, newest first (append on loadMore)
 *   - {@code unreadCount}  — drives the badge on the bottom bar
 *   - {@code loading}      — distinguishes "no data" from "still fetching"
 *   - {@code refreshing}   — true only during a pull-to-refresh
 *   - {@code loadingMore}  — true only while appending the next page
 *   - {@code endReached}   — true when the server has no more pages
 *   - {@code filter}       — current filter (ALL / UNREAD_ONLY / by type)
 *   - {@code error}        — non-blocking error stamp; UI shows snackbar
 *   - {@code responses}    — Map of notificationId -> "ACCEPTED" / "DECLINED"
 *   - {@code responding}   — true while a respond() call is in flight
 *   - {@code events}       — current detail-dialog event trail (or null)
 *   - {@code loadingEvents} — true while the events fetch is in flight
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val dataSource: NotificationsDataSource
) : ViewModel() {

    /** Filter modes for the feed. */
    enum class Filter {
        ALL, UNREAD_ONLY
    }

    data class State(
        val items: List<NotificationViewDto> = emptyList(),
        val unreadCount: Long = 0L,
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val page: Int = 0,
        val filter: Filter = Filter.ALL,
        val error: String? = null,
        val responses: Map<Long, String> = emptyMap(),
        val responding: Set<Long> = emptySet(),
        val events: List<NotificationEventViewDto> = emptyList(),
        val loadingEvents: Boolean = false,
        val eventsForNotificationId: Long? = null,
    )

    private val pageSize: Int = 20

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** First load. Replaces existing items. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, endReached = false, page = 0) }
            loadPage(reset = true)
        }
    }

    /**
     * Phase F — pull-to-refresh entry point. Distinct from [refresh] so the
     * UI can show a small spinner in the swipe header instead of replacing
     * the entire feed with a centred progress indicator.
     */
    fun pullToRefresh() {
        if (_state.value.refreshing || _state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null, endReached = false, page = 0) }
            loadPage(reset = true)
        }
    }

    /** Phase F — append the next page when the user scrolls to the bottom. */
    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.endReached || s.loading || s.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, page = it.page + 1) }
            loadPage(reset = false)
        }
    }

    /** Phase F — switch between ALL and UNREAD_ONLY. Resets the list. */
    fun setFilter(filter: Filter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    private suspend fun loadPage(reset: Boolean) {
        runCatching {
            val nextPage = if (reset) 0 else _state.value.page
            val resp = dataSource.list(page = nextPage, size = pageSize)
            val count = dataSource.unreadCount()
            val responseMap = mutableMapOf<Long, String>()
            val items = resp?.items.orEmpty()
            items.forEach { n ->
                n.id?.let { id ->
                    val parsed = NotificationPayload.parseVerdict(n.payloadJson)
                    if (parsed != null) responseMap[id] = parsed
                }
            }
            val filtered = applyFilter(items, _state.value.filter)
            val totalPages = resp?.totalPages ?: 0
            val endReached = nextPage + 1 >= totalPages || items.isEmpty()
            _state.update { st ->
                st.copy(
                    items = if (reset) filtered else st.items + filtered,
                    unreadCount = count,
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                    endReached = endReached,
                    page = nextPage,
                    responses = if (reset) responseMap else st.responses + responseMap,
                    error = null,
                )
            }
        }.onFailure { ex ->
            _state.update { it.copy(loading = false, refreshing = false, loadingMore = false, error = ex.message ?: "error") }
        }
    }

    private fun applyFilter(items: List<NotificationViewDto>, filter: Filter): List<NotificationViewDto> {
        return when (filter) {
            Filter.ALL -> items
            Filter.UNREAD_ONLY -> items.filter { it.readAt.isNullOrBlank() }
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            if (dataSource.markRead(id)) {
                _state.update { st ->
                    val newItems = st.items.map { n ->
                        if (n.id == id && n.readAt == null) n.copy(readAt = "now") else n
                    }
                    // Drop from UNREAD_ONLY if that filter is active.
                    val visible = if (st.filter == Filter.UNREAD_ONLY) newItems.filter { it.readAt.isNullOrBlank() } else newItems
                    st.copy(
                        items = visible,
                        unreadCount = (st.unreadCount - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val n = dataSource.markAllRead()
            _state.update { st ->
                val newItems = st.items.map { it.copy(readAt = it.readAt ?: "now") }
                val visible = if (st.filter == Filter.UNREAD_ONLY) emptyList() else newItems
                st.copy(
                    items = visible,
                    unreadCount = (st.unreadCount - n).coerceAtLeast(0)
                )
            }
        }
    }

    /**
     * V2.3 / V18 — Send ACCEPTED / DECLINED verdict to the server. The server
     * is idempotent, so we don't gate on already-responded; if the user taps
     * Accept twice the second POST is a no-op and the UI just refreshes.
     */
    fun respond(notificationId: Long, verdict: String) {
        if (verdict != "ACCEPTED" && verdict != "DECLINED") return
        viewModelScope.launch {
            _state.update { it.copy(responding = it.responding + notificationId) }
            val saved = dataSource.respond(notificationId, verdict)
            _state.update {
                val next = it.responding - notificationId
                if (saved != null) {
                    it.copy(
                        responses = it.responses + (notificationId to saved),
                        responding = next
                    )
                } else {
                    it.copy(responding = next, error = "respond_failed")
                }
            }
        }
    }

    /** Phase F — load and surface the audit trail for one notification. */
    fun openEvents(notificationId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loadingEvents = true, eventsForNotificationId = notificationId, events = emptyList()) }
            runCatching { dataSource.events(notificationId) }
                .onSuccess { list ->
                    _state.update { it.copy(loadingEvents = false, events = list) }
                }
                .onFailure { ex ->
                    _state.update { it.copy(loadingEvents = false, error = ex.message ?: "error") }
                }
        }
    }

    /** Phase F — clear events dialog state. */
    fun closeEvents() {
        _state.update { it.copy(events = emptyList(), eventsForNotificationId = null, loadingEvents = false) }
    }

    /** Phase F — let the UI ack an error (e.g. snackbar dismissed). */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}