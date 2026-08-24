package com.restaurant.staff.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.i18n.LocaleHolder
import com.restaurant.staff.network.FoodViewDto
import com.restaurant.staff.network.StoreViewDto
import com.restaurant.staff.repository.FoodRepository
import com.restaurant.staff.repository.StoreRepository
import com.restaurant.staff.storage.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val foods: FoodRepository,
    private val store: StoreRepository,
    private val session: SessionStore,
    localeHolder: LocaleHolder
) : ViewModel() {

    data class UiState(
        val fullName: String? = null,
        val storeName: String? = null,
        val featured: List<FoodViewDto> = emptyList(),
        val popular: List<FoodViewDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    )

    private data class Quad<A,B,C,D>(val a:A, val b:B, val c:C, val d:D)

    private val lang = localeHolder.language(viewModelScope)
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val currentLang = lang.value
            runCatching {
                val user = session.user.first()
                val featured = foods.featured(currentLang, 10)
                val popular = foods.list(currentLang, page = 0, size = 12).items.orEmpty()
                val store = store.get(currentLang)
                Quad(user, store, featured, popular)
            }.fold(
                onSuccess = { q ->
                    _state.update {
                        it.copy(
                            fullName = q.user?.fullName,
                            storeName = q.store?.name,
                            featured = q.featured,
                            popular = q.popular,
                            loading = false,
                            error = null
                        )
                    }
                },
                onFailure = { ex ->
                    _state.update { it.copy(loading = false, error = ex.message ?: "error") }
                }
            )
        }
    }
}