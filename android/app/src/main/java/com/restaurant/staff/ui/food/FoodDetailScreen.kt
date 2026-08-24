package com.restaurant.staff.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.restaurant.staff.R
import com.restaurant.staff.ui.theme.RestaurantStaffTheme

@Composable
fun FoodDetailScreen(
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                }
                state.food == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.common_no_data))
                }
                else -> {
                    val f = state.food!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        val image = f.imageUrl
                        if (image != null && image.isNotBlank()) {
                            AsyncImage(
                                model = image,
                                contentDescription = f.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(text = f.name ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(text = "₫${f.price ?: "0"}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        if (!f.categoryName.isNullOrBlank()) {
                            Text(text = "${stringResource(id = R.string.food_category_label)}: ${f.categoryName}",
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!f.status.isNullOrBlank()) {
                            val statusText = when (f.status) {
                                "AVAILABLE" -> stringResource(id = R.string.menu_status_available)
                                "SOLD_OUT" -> stringResource(id = R.string.menu_status_sold_out)
                                "HIDDEN" -> stringResource(id = R.string.menu_status_hidden)
                                else -> f.status
                            }
                            Text(
                                text = "${stringResource(id = R.string.food_status_label)}: $statusText",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (f.featured == true) {
                            Text(
                                text = stringResource(id = R.string.menu_featured_badge),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (!f.description.isNullOrBlank()) {
                            Text(text = stringResource(id = R.string.food_description), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(text = f.description!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                        }
                        if (!f.ingredients.isNullOrBlank()) {
                            Text(text = stringResource(id = R.string.food_ingredients), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(text = f.ingredients!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                        }
                        if (!f.portion.isNullOrBlank()) {
                            Text(text = stringResource(id = R.string.food_portion), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(text = f.portion!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}