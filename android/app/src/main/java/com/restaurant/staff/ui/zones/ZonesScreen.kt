package com.restaurant.staff.ui.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.restaurant.staff.R
import com.restaurant.staff.network.ZoneViewDto

@Composable
fun ZonesScreen(
    viewModel: ZonesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang = "vi" // local fallback; full i18n handled server-side

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.zones_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        state.current?.let { current ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("${stringResource(R.string.zones_current)}: " +
                            (current.zoneName ?: current.zoneCode ?: "?"),
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.zones, key = { it.id ?: 0L }) { z ->
                ZoneCard(
                    z = z,
                    lang = lang,
                    isCurrent = z.currentAssignment == true,
                    isWorking = state.workingZoneId == z.id,
                    onAssign = { viewModel.selfAssign(z.id ?: return@ZoneCard) }
                )
            }
        }
    }
}

@Composable
private fun ZoneCard(
    z: ZoneViewDto,
    lang: String,
    isCurrent: Boolean,
    isWorking: Boolean,
    onAssign: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(parseColor(z.color))
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(z.translations?.firstOrNull { it.lang == lang }?.name
                        ?: z.translations?.firstOrNull()?.name
                        ?: z.code ?: "?",
                    style = MaterialTheme.typography.titleMedium)
                Text(z.code ?: "?", style = MaterialTheme.typography.bodySmall)
            }
            if (isCurrent) {
                Text(stringResource(R.string.zones_current_badge),
                    color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onAssign, enabled = !isWorking) {
                    Text(stringResource(R.string.zones_assign))
                }
            }
        }
    }
}

private fun parseColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF3B82F6)
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }
}
