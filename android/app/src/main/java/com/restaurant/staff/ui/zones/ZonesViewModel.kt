package com.restaurant.staff.ui.zones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.network.ZoneAssignmentDto
import com.restaurant.staff.network.ZoneViewDto
import com.restaurant.staff.repository.V22Repository
import com.restaurant.staff.storage.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.2 — staff-side zone browser. Lists every ACTIVE zone (with the
 * "currentAssignment" flag set on the row the user is in right now), lets
 * the user move themselves into a different zone, and shows their recent
 * assignment history.
 */
@HiltViewModel
class ZonesViewModel @Inject constructor(
    private val repo: V22Repository,
    private val session: SessionStore
) : ViewModel() {

    data class UiState(
        val zones: List<ZoneViewDto> = emptyList(),
        val current: ZoneAssignmentDto? = null,
        val history: List<ZoneAssignmentDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val workingZoneId: Long? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val zones = repo.listZones()
                val current = repo.currentZone()
                val history = repo.zoneHistory()
                Triple(zones, current, history)
            }.onSuccess { (zones, current, history) ->
                _state.update { it.copy(zones = zones, current = current, history = history, loading = false) }
            }.onFailure { ex ->
                _state.update { it.copy(loading = false, error = ex.message ?: "error") }
            }
        }
    }

    fun selfAssign(zoneId: Long) {
        _state.update { it.copy(workingZoneId = zoneId) }
        viewModelScope.launch {
            runCatching {
                val user = session.user.first()
                    ?: throw IllegalStateException("Not signed in")
                repo.selfAssignZone(user.id, zoneId, reason = "self-service")
            }.onSuccess {
                _state.update { it.copy(workingZoneId = null) }
                refresh()
            }.onFailure { ex ->
                _state.update { it.copy(workingZoneId = null, error = ex.message ?: "error") }
            }
        }
    }
}
