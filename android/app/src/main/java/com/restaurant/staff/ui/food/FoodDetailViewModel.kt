package com.restaurant.staff.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.i18n.LocaleHolder
import com.restaurant.staff.network.FoodViewDto
import com.restaurant.staff.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foods: FoodRepository,
    localeHolder: LocaleHolder
) : ViewModel() {

    data class UiState(
        val id: Long? = null,
        val food: FoodViewDto? = null,
        val loading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val lang = localeHolder.language(viewModelScope)

    init {
        val id = savedStateHandle.get<String>("id")?.toLongOrNull()
        _state.update { it.copy(id = id) }
        if (id != null) load(id)
    }

    fun load(id: Long) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { foods.detail(lang.value, id) }
                .fold(
                    onSuccess = { f -> _state.update { it.copy(loading = false, food = f) } },
                    onFailure = { ex -> _state.update { it.copy(loading = false, error = ex.message) } }
                )
        }
    }
}