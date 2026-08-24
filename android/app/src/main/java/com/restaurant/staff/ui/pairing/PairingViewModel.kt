package com.restaurant.staff.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.network.QrPayloadParser
import com.restaurant.staff.storage.ServerConfig
import com.restaurant.staff.storage.ServerConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val configStore: ServerConfigStore,
    private val apiProvider: ApiClientProvider
) : ViewModel() {

    data class UiState(
        val scanning: Boolean = false,
        val manualHost: String = "",
        val manualPort: String = "8080",
        val testing: Boolean = false,
        val testResult: TestResult? = null,
        val savedConfig: ServerConfig? = null,
        val errorMessageKey: String? = null
    )

    sealed interface TestResult {
        data class Success(val version: String?) : TestResult
        data class Failed(val reason: String) : TestResult
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configStore.current()?.let { cfg ->
                _state.update { s -> s.copy(savedConfig = cfg) }
            }
        }
    }

    fun startScan() = _state.update { it.copy(scanning = true, testResult = null, errorMessageKey = null) }
    fun stopScan() = _state.update { it.copy(scanning = false) }

    fun updateManualHost(value: String) = _state.update { it.copy(manualHost = value) }
    fun updateManualPort(value: String) = _state.update {
        it.copy(manualPort = value.filter { c -> c.isDigit() }.take(5))
    }

    fun onQrText(raw: String, onResolved: () -> Unit) {
        val payload = QrPayloadParser.parse(raw)
        if (payload == null) {
            _state.update { it.copy(testResult = TestResult.Failed("QR_INVALID"), scanning = false) }
            return
        }
        _state.update {
            it.copy(
                manualHost = payload.server,
                manualPort = payload.port.toString(),
                scanning = false
            )
        }
        testAndSave(payload.server, payload.port, payload.protocol, onResolved)
    }

    fun connectManually(onResolved: () -> Unit) {
        val host = _state.value.manualHost.trim()
        val port = _state.value.manualPort.trim().toIntOrNull() ?: 8080
        if (host.isEmpty()) {
            _state.update { it.copy(testResult = TestResult.Failed("HOST_REQUIRED")) }
            return
        }
        testAndSave(host, port, "http", onResolved)
    }

    private fun testAndSave(host: String, port: Int, protocol: String, onResolved: () -> Unit) {
        _state.update { it.copy(testing = true, testResult = null, errorMessageKey = null) }
        viewModelScope.launch {
            val cfg = ServerConfig(host = host, port = port, protocol = protocol)
            runCatching {
                val api = apiProvider.serverApiFor(cfg)
                val resp = api.health()
                if (resp.success == true) resp.data?.version else null
            }.fold(
                onSuccess = { version ->
                    val finalCfg = cfg.copy(serverVersion = version)
                    configStore.save(finalCfg)
                    _state.update {
                        it.copy(
                            testing = false,
                            testResult = TestResult.Success(version),
                            savedConfig = finalCfg
                        )
                    }
                    onResolved()
                },
                onFailure = { ex ->
                    _state.update {
                        it.copy(
                            testing = false,
                            testResult = TestResult.Failed(ex.message ?: ex::class.java.simpleName)
                        )
                    }
                }
            )
        }
    }

    fun clearSavedConfig() {
        viewModelScope.launch {
            configStore.clear()
            _state.update { it.copy(savedConfig = null, testResult = null) }
        }
    }
}