package com.restaurant.staff.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.i18n.LocaleHolder
import com.restaurant.staff.network.CategoryViewDto
import com.restaurant.staff.network.FoodViewDto
import com.restaurant.staff.repository.CategoryRepository
import com.restaurant.staff.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val categories: CategoryRepository,
    private val foods: FoodRepository,
    localeHolder: LocaleHolder
) : ViewModel() {

    data class UiState(
        val categories: List<CategoryViewDto> = emptyList(),
        val selectedCategoryId: Long? = null,
        val query: String = "",
        val items: List<FoodViewDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    )

    private val lang = localeHolder.language(viewModelScope)
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init { loadCategories() }

    fun loadCategories() {
        viewModelScope.launch {
            runCatching { categories.list(lang.value) }
                .onSuccess { list -> _state.update { it.copy(categories = list) } }
                .onFailure { ex -> _state.update { it.copy(error = ex.message) } }
            loadFoods()
        }
    }

    fun selectCategory(id: Long?) {
        _state.update { it.copy(selectedCategoryId = id) }
        loadFoods()
    }

    fun onQuery(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            loadFoods()
        }
    }

    fun loadFoods() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                foods.list(
                    lang = lang.value,
                    q = s.query.trim().takeIf { it.isNotEmpty() },
                    categoryId = s.selectedCategoryId,
                    page = 0, size = 100
                ).items.orEmpty()
            }.fold(
                onSuccess = { list ->
                    _state.update { it.copy(loading = false, items = list, error = null) }
                },
                onFailure = { ex ->
                    _state.update { it.copy(loading = false, error = ex.message) }
                }
            )
        }
    }
}