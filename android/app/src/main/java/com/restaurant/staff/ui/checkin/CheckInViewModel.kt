package com.restaurant.staff.ui.checkin

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.network.CheckInDto
import com.restaurant.staff.network.ZoneViewDto
import com.restaurant.staff.repository.V22Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.2 — check-in / check-out screen. The user picks a zone and toggles
 * between CHECK_IN and CHECK_OUT. The server enforces "exactly one open
 * check-in per user" so we surface the error message verbatim.
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: V22Repository
) : ViewModel() {

    data class UiState(
        val zones: List<ZoneViewDto> = emptyList(),
        val recent: List<CheckInDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val working: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val zones = repo.listZones()
                val recent = repo.recentCheckIns(20)
                zones to recent
            }.onSuccess { (zones, recent) ->
                _state.update { it.copy(zones = zones, recent = recent, loading = false) }
            }.onFailure { ex ->
                _state.update { it.copy(loading = false, error = ex.message ?: "error") }
            }
        }
    }

    fun toggle(zoneId: Long, action: String) {
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.checkIn(zoneId = zoneId, action = action, notes = null, deviceId = deviceId())
            }.onSuccess {
                _state.update { it.copy(working = false) }
                refresh()
            }.onFailure { ex ->
                _state.update { it.copy(working = false, error = ex.message ?: "error") }
            }
        }
    }

    private fun deviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "unknown"
        }
    }
}
