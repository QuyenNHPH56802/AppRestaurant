package com.restaurant.staff.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.restaurant.staff.R
import com.restaurant.staff.notifications.NotificationEventViewDto
import com.restaurant.staff.notifications.NotificationViewDto
import com.restaurant.staff.notifications.NotificationsViewModel

/**
 * V2.3 / V18 — Phase F — Notification feed screen.
 *
 * Two entry modes:
 *   - List mode (openNotificationId == null): all notifications, newest
 *     first, with a "mark all read" action.
 *   - Detail mode (openNotificationId != null): we still load the list, then
 *     auto-open the one matching the id passed from the notification tap.
 *     Auto-marking read on entry mirrors the system's behavior and means
 *     the user can close the phone and not worry about cleaning up the
 *     badge.
 *
 * For SHIFT_ASSIGNED rows we render Accept / Decline buttons. Both POST to
 * {@code /api/me/notifications/{id}/respond}; the server is idempotent so
 * the user can change their mind and the verdict just flips.
 *
 * Phase F additions:
 *   - Pull-to-refresh (SwipeRefresh wrapper)
 *   - ALL / UNREAD_ONLY filter chips
 *   - "Events" action that opens an audit-trail dialog (one row per delivery attempt)
 *   - Snackbar host for non-blocking errors
 *   - Pagination via [NotificationsViewModel.loadMore]
 *   - Empty state with retry button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenNotification: (Long) -> Unit,
    openNotificationId: Long? = null,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    LaunchedEffect(state.items, openNotificationId) {
        openNotificationId?.let { id ->
            if (state.items.any { it.id == id && it.readAt == null }) {
                viewModel.markRead(id)
            }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHost.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null)
                            Text(
                                text = stringResource(R.string.notifications_mark_all_read),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                FilterBar(
                    current = state.filter,
                    onChange = { viewModel.setFilter(it) }
                )
                when {
                    state.loading && state.items.isEmpty() -> CenteredProgress()
                    state.items.isEmpty() -> EmptyState(
                        filter = state.filter,
                        onRetry = { viewModel.refresh() }
                    )
                    else -> NotificationList(
                        state = state,
                        viewModel = viewModel,
                        onOpenNotification = onOpenNotification
                    )
                }
            }
            // Pull-to-refresh spinner overlay (top).
            if (state.refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .size(28.dp)
                )
            }
        }
    }

    // Phase F — events dialog (audit trail per notification).
    if (state.eventsForNotificationId != null) {
        EventsDialog(state = state, onDismiss = { viewModel.closeEvents() })
    }
}

@Composable
private fun FilterBar(
    current: NotificationsViewModel.Filter,
    onChange: (NotificationsViewModel.Filter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
        FilterChip(
            selected = current == NotificationsViewModel.Filter.ALL,
            onClick = { onChange(NotificationsViewModel.Filter.ALL) },
            label = { Text(stringResource(R.string.notifications_filter_all)) }
        )
        FilterChip(
            selected = current == NotificationsViewModel.Filter.UNREAD_ONLY,
            onClick = { onChange(NotificationsViewModel.Filter.UNREAD_ONLY) },
            label = { Text(stringResource(R.string.notifications_filter_unread)) }
        )
    }
}

@Composable
private fun NotificationList(
    state: NotificationsViewModel.State,
    viewModel: NotificationsViewModel,
    onOpenNotification: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.items, key = { it.id ?: 0L }) { n ->
            NotificationRow(
                n = n,
                response = n.id?.let { state.responses[it] },
                responding = n.id?.let { state.responding.contains(it) } == true,
                onClick = { id ->
                    viewModel.markRead(id)
                    onOpenNotification(id)
                },
                onAccept = { id -> viewModel.respond(id, "ACCEPTED") },
                onDecline = { id -> viewModel.respond(id, "DECLINED") },
                onShowEvents = { id -> viewModel.openEvents(id) }
            )
        }
        // Phase F — pagination footer
        item {
            when {
                state.loadingMore -> Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.common_loading))
                }
                !state.endReached && state.items.isNotEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(onClick = { viewModel.loadMore() }) {
                        Text(stringResource(R.string.notifications_load_more))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    filter: NotificationsViewModel.Filter,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val msg = when (filter) {
                NotificationsViewModel.Filter.ALL -> stringResource(R.string.notifications_empty)
                NotificationsViewModel.Filter.UNREAD_ONLY -> stringResource(R.string.notifications_empty_unread)
            }
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}

/**
 * Server-side notification types that can be answered with Accept/Decline.
 * Add new types here (e.g. SHIFT_SWAP_REQUESTED) to make the buttons appear.
 */
private val RespondableTypes = setOf(
    "SHIFT_ASSIGNED",
    "SHIFT_SWAP_REQUESTED"
)

@Composable
private fun NotificationRow(
    n: NotificationViewDto,
    response: String?,
    responding: Boolean,
    onClick: (Long) -> Unit,
    onAccept: (Long) -> Unit,
    onDecline: (Long) -> Unit,
    onShowEvents: (Long) -> Unit
) {
    val isRead = !n.readAt.isNullOrBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = { n.id?.let(onClick) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Phase F — title row with optional type chip + history button.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = n.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                n.type?.let { type ->
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                n.id?.let { id ->
                    IconButton(onClick = { onShowEvents(id) }) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.notifications_events))
                    }
                }
            }
            if (!n.body.isNullOrBlank()) {
                Text(
                    text = n.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            n.createdAt?.let { createdAt ->
                Text(
                    text = createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            // V2.3 / V18 — Action row for respondable types. Disabled while a
            // request is in flight so the user can't double-fire and the UI
            // doesn't flip-flop between enabled states.
            val type = n.type ?: ""
            if (type in RespondableTypes && n.id != null) {
                val id = n.id
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAccepted = response == "ACCEPTED"
                    val isDeclined = response == "DECLINED"
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !responding && response == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = { onAccept(id) }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text(
                            text = if (isAccepted) stringResource(R.string.notification_action_accepted)
                                   else stringResource(R.string.notification_action_accept),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !responding && response == null,
                        onClick = { onDecline(id) }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Text(
                            text = if (isDeclined) stringResource(R.string.notification_action_declined)
                                   else stringResource(R.string.notification_action_decline),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                if (response != null && !responding) {
                    Text(
                        text = stringResource(
                            if (response == "ACCEPTED") R.string.notification_action_accepted_hint
                            else R.string.notification_action_declined_hint
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventsDialog(
    state: NotificationsViewModel.State,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notifications_events_title)) },
        text = {
            when {
                state.loadingEvents -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.events.isEmpty() -> Text(stringResource(R.string.notifications_events_empty))
                else -> LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(state.events, key = { it.id ?: 0L }) { e ->
                        EventRow(e)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}

@Composable
private fun EventRow(e: NotificationEventViewDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${e.channel ?: "?"} · ${e.status ?: "?"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "#${e.attempts ?: 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        e.errorCode?.takeIf { it.isNotBlank() }?.let { code ->
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        e.lastAttemptAt?.let { ts ->
            Text(
                text = ts,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}