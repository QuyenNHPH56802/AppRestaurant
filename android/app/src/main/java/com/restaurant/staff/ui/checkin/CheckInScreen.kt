package com.restaurant.staff.ui.checkin

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.restaurant.staff.R
import com.restaurant.staff.network.CheckInDto
import com.restaurant.staff.network.ZoneViewDto

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang = "vi" // locale injected by repository via ?lang= on the API calls

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.checkin_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (state.loading) {
            CircularProgressIndicator()
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.zones, key = { it.id ?: 0L }) { z ->
                ZoneToggle(
                    z = z,
                    lang = lang,
                    working = state.working,
                    onCheckIn = { z.id?.let { id -> viewModel.toggle(id, "CHECK_IN") } },
                    onCheckOut = { z.id?.let { id -> viewModel.toggle(id, "CHECK_OUT") } }
                )
            }
        }

        if (state.recent.isNotEmpty()) {
            Text(
                stringResource(R.string.checkin_recent),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.recent, key = { it.id ?: 0L }) { item ->
                    RecentRow(item)
                }
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ZoneToggle(
    z: ZoneViewDto,
    lang: String,
    working: Boolean,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val label = z.translations?.firstOrNull { it.lang == lang }?.name
        ?: z.translations?.firstOrNull()?.name
        ?: z.code
        ?: "?"
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCheckIn, enabled = !working) {
                Text(stringResource(R.string.checkin_in))
            }
            OutlinedButton(
                onClick = onCheckOut,
                enabled = !working,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(stringResource(R.string.checkin_out))
            }
        }
    }
}

@Composable
private fun RecentRow(item: CheckInDto) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.zoneCode ?: "?", modifier = Modifier.weight(1f))
            Text(item.action ?: "?")
            item.createdAt?.take(19)?.let {
                Text(it, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
