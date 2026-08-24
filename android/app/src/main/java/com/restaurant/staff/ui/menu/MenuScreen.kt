package com.restaurant.staff.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.restaurant.staff.R
import com.restaurant.staff.network.FoodViewDto
import com.restaurant.staff.ui.theme.RestaurantStaffTheme

@Composable
fun MenuScreen(
    onFoodClick: (Long) -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.menu_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    placeholder = { Text(stringResource(id = R.string.menu_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(stringResource(id = R.string.menu_category_all)) },
                            colors = chipColors(state.selectedCategoryId == null)
                        )
                    }
                    items(state.categories) { c ->
                        AssistChip(
                            onClick = { viewModel.selectCategory(c.id) },
                            label = { Text(c.name ?: "—") },
                            colors = chipColors(state.selectedCategoryId == c.id)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.loading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                        state.error != null -> Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        state.items.isEmpty() -> Text(
                            text = stringResource(id = R.string.menu_empty),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.items) { food ->
                                FoodListItem(food, onClick = { food.id?.let(onFoodClick) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = AssistChipDefaults.assistChipColors(
    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
)

@Composable
private fun FoodListItem(food: FoodViewDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            val image = food.imageUrl
            if (image != null && image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = food.name,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {}
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = food.name ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (food.featured == true) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(id = R.string.menu_featured_badge),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = food.categoryName ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₫${food.price ?: "0"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusBadge(status = food.status)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val (text, color) = when (status) {
        "AVAILABLE" -> stringResource(id = R.string.menu_status_available) to MaterialTheme.colorScheme.secondary
        "SOLD_OUT" -> stringResource(id = R.string.menu_status_sold_out) to MaterialTheme.colorScheme.error
        "HIDDEN" -> stringResource(id = R.string.menu_status_hidden) to Color.Gray
        else -> "" to Color.Gray
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}