package com.restaurant.staff.ui.checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.network.ChecklistCompletionDto
import com.restaurant.staff.network.ChecklistViewDto
import com.restaurant.staff.repository.V22Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.2 — staff checklists. Lists active checklists (optionally filtered by
 * zone) and lets the user mark each task COMPLETED or SKIPPED. Records the
 * server response so the UI can show recent activity.
 */
@HiltViewModel
class ChecklistsViewModel @Inject constructor(
    private val repo: V22Repository
) : ViewModel() {

    data class UiState(
        val checklists: List<ChecklistViewDto> = emptyList(),
        val completions: List<ChecklistCompletionDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val workingTaskId: Long? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val lists = repo.listChecklists(zoneId = null)
                val recent = repo.recentCompletions(20)
                lists to recent
            }.onSuccess { (lists, recent) ->
                _state.update { it.copy(checklists = lists, completions = recent, loading = false) }
            }.onFailure { ex ->
                _state.update { it.copy(loading = false, error = ex.message ?: "error") }
            }
        }
    }

    fun complete(taskId: Long, status: String, notes: String? = null) {
        _state.update { it.copy(workingTaskId = taskId) }
        viewModelScope.launch {
            runCatching { repo.completeChecklistTask(taskId, status, notes, shiftId = null) }
                .onSuccess { completion ->
                    _state.update { st ->
                        st.copy(
                            workingTaskId = null,
                            completions = (listOf(completion) + st.completions).take(20)
                        )
                    }
                }
                .onFailure { ex ->
                    _state.update { it.copy(workingTaskId = null, error = ex.message ?: "error") }
                }
        }
    }
}
