package com.restaurant.staff.ui.shifts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.restaurant.staff.R
import com.restaurant.staff.network.ShiftAssignmentViewDto

@Composable
fun ShiftsScreen(
    viewModel: ShiftsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.shifts_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (state.loading) {
            CircularProgressIndicator()
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state.items.isEmpty() && !state.loading) {
            Text(stringResource(R.string.shifts_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.id ?: 0L }) { item ->
                    ShiftCard(item = item, working = state.workingId == item.id,
                        onAccept = { viewModel.respond(item.id ?: return@ShiftCard, "ACCEPTED") },
                        onReject = { viewModel.respond(item.id ?: return@ShiftCard, "REJECTED") },
                        onChange = { viewModel.respond(item.id ?: return@ShiftCard, "CHANGE_REQUESTED") },
                        onCancel = { viewModel.respond(item.id ?: return@ShiftCard, "CANCELLED") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftCard(
    item: ShiftAssignmentViewDto,
    working: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onChange: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.shiftName ?: "?"} (${item.shiftStartTime ?: "?"}-${item.shiftEndTime ?: "?"})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(item.status ?: "?") }
                )
            }
            Text("${stringResource(R.string.shifts_date)}: ${item.date ?: "?"}")
            if (!item.notes.isNullOrBlank()) {
                Text("${stringResource(R.string.shifts_notes)}: ${item.notes}",
                    style = MaterialTheme.typography.bodySmall)
            }

            val isPending = item.status == "SCHEDULED" || item.status == "CONFIRMED"
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !working
                    ) { Text(stringResource(R.string.shifts_accept)) }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !working
                    ) { Text(stringResource(R.string.shifts_reject)) }
                    OutlinedButton(
                        onClick = onChange,
                        enabled = !working
                    ) { Text(stringResource(R.string.shifts_change)) }
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !working
                    ) { Text(stringResource(R.string.shifts_cancel)) }
                }
            }
        }
    }
}
