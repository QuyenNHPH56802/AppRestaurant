package com.restaurant.staff.ui.checklists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.restaurant.staff.R
import com.restaurant.staff.network.ChecklistTaskDto
import com.restaurant.staff.network.ChecklistViewDto

@Composable
fun ChecklistsScreen(
    viewModel: ChecklistsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang = "vi"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.checklists_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.checklists.isEmpty() && !state.loading) {
            Text(stringResource(R.string.checklists_empty))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.checklists, key = { it.id ?: 0L }) { c ->
                ChecklistCard(
                    checklist = c,
                    lang = lang,
                    workingTaskId = state.workingTaskId,
                    onComplete = { taskId, status ->
                        viewModel.complete(taskId, status)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistCard(
    checklist: ChecklistViewDto,
    lang: String,
    workingTaskId: Long?,
    onComplete: (Long, String) -> Unit
) {
    val title = checklist.translations?.firstOrNull { it.lang == lang }?.title
        ?: checklist.translations?.firstOrNull()?.title
        ?: checklist.zoneName
        ?: "?"

    Card(modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(checklist.zoneName ?: checklist.zoneCode ?: "",
                style = MaterialTheme.typography.bodySmall)
            Column(modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (checklist.tasks ?: emptyList()).forEach { t ->
                    TaskRow(
                        task = t,
                        lang = lang,
                        working = workingTaskId == t.id,
                        onComplete = { status -> t.id?.let { onComplete(it, status) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: ChecklistTaskDto,
    lang: String,
    working: Boolean,
    onComplete: (String) -> Unit
) {
    val title = task.translations?.firstOrNull { it.lang == lang }?.title
        ?: task.translations?.firstOrNull()?.title
        ?: "?"

    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Button(
            onClick = { onComplete("COMPLETED") },
            enabled = !working
        ) { Text(stringResource(R.string.checklists_done)) }
        if (task.required != true) {
            OutlinedButton(
                onClick = { onComplete("SKIPPED") },
                enabled = !working,
                modifier = Modifier.padding(start = 4.dp)
            ) { Text(stringResource(R.string.checklists_skip)) }
        }
    }
}
