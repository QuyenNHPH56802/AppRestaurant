package com.restaurant.staff.ui.shifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.network.ShiftAssignmentViewDto
import com.restaurant.staff.repository.V22Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.2 — list of upcoming / historical shift assignments for the signed-in
 * user. Staff can accept / reject / request-change / cancel pending rows.
 */
@HiltViewModel
class ShiftsViewModel @Inject constructor(
    private val repo: V22Repository
) : ViewModel() {

    data class UiState(
        val items: List<ShiftAssignmentViewDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val workingId: Long? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.listShifts() }
                .onSuccess { items ->
                    _state.update { it.copy(items = items, loading = false, error = null) }
                }
                .onFailure { ex ->
                    _state.update { it.copy(loading = false, error = ex.message ?: "error") }
                }
        }
    }

    fun respond(id: Long, status: String, notes: String? = null) {
        _state.update { it.copy(workingId = id) }
        viewModelScope.launch {
            runCatching { repo.respondShift(id, status, notes) }
                .onSuccess { updated ->
                    _state.update { st ->
                        st.copy(
                            workingId = null,
                            items = st.items.map { if (it.id == updated.id) updated else it }
                        )
                    }
                }
                .onFailure { ex ->
                    _state.update { it.copy(workingId = null, error = ex.message ?: "error") }
                }
        }
    }
}
