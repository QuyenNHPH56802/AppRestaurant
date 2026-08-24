package com.restaurant.staff.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restaurant.staff.R
import com.restaurant.staff.kiosk.KioskController
import com.restaurant.staff.storage.KioskSettingsStore
import com.restaurant.staff.storage.LocaleStore
import com.restaurant.staff.ui.kiosk.AdminPinDialog
import com.restaurant.staff.ui.theme.RestaurantStaffTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localeStore: LocaleStore,
    private val kioskStore: KioskSettingsStore,
) : ViewModel() {

    data class UiState(
        val language: String = "vi",
        val kioskEnabled: Boolean = false,
        val hasPin: Boolean = false,
        val isDeviceOwner: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun bindKiosk(controller: KioskController) {
        _state.update { it.copy(isDeviceOwner = controller.isDeviceOwner) }
    }

    init {
        viewModelScope.launch {
            localeStore.language.collect { lang -> _state.update { it.copy(language = lang) } }
        }
        viewModelScope.launch {
            kioskStore.enabled.collect { v -> _state.update { it.copy(kioskEnabled = v) } }
        }
        viewModelScope.launch {
            kioskStore.adminPin.collect { v -> _state.update { it.copy(hasPin = !v.isNullOrBlank()) } }
        }
    }

    fun setLanguage(lang: String) { viewModelScope.launch { localeStore.set(lang) } }
    fun setKioskEnabled(value: Boolean) { viewModelScope.launch { kioskStore.setEnabled(value) } }
    fun savePin(pin: String) { viewModelScope.launch { kioskStore.setPin(pin) } }
}

@Composable
fun SettingsScreen(
    onOpenPairing: () -> Unit,
    onOpenShifts: () -> Unit = {},
    onOpenZones: () -> Unit = {},
    onOpenChecklists: () -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    kiosk: KioskController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    LaunchedEffect(Unit) { viewModel.bindKiosk(kiosk) }

    var showSetPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LanguageSection(current = state.language, onChange = viewModel::setLanguage)
                KioskSection(
                    enabled = state.kioskEnabled,
                    hasPin = state.hasPin,
                    isDeviceOwner = state.isDeviceOwner,
                    onToggle = { on ->
                        // Require PIN before enabling
                        if (on && !state.hasPin) {
                            showSetPinDialog = true
                            pinError = null
                            return@KioskSection
                        }
                        if (on && activity != null && kiosk.isDeviceOwner) {
                            kiosk.setLockTaskFeatures(0)
                            kiosk.applyKioskRestrictions()
                            kiosk.startLockTask(activity)
                        }
                        viewModel.setKioskEnabled(on)
                    },
                    onSetPin = { showSetPinDialog = true; pinError = null }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenPairing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.pairing_settings_server))
                }
                // V2.2 — operational screens reachable from settings so
                // bottom-nav stays lean for the food browsing flow.
                OutlinedButton(onClick = onOpenShifts, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.shifts_title))
                }
                OutlinedButton(onClick = onOpenZones, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.zones_title))
                }
                OutlinedButton(onClick = onOpenChecklists, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.checklists_title))
                }
                OutlinedButton(onClick = onOpenCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.checkin_title))
                }
            }
        }
    }

    if (showSetPinDialog) {
        AdminPinDialog(
            title = "Đặt mã PIN quản trị (4–8 số)",
            confirmLabel = "Lưu",
            errorMessage = pinError,
            onConfirm = { pin ->
                if (pin.length < 4) {
                    pinError = "PIN phải có ít nhất 4 số"
                    return@AdminPinDialog
                }
                viewModel.savePin(pin)
                // If toggling on via dialog, also enable
                viewModel.setKioskEnabled(true)
                showSetPinDialog = false
            },
            onDismiss = { showSetPinDialog = false }
        )
    }
}

@Composable
private fun LanguageSection(current: String, onChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(id = R.string.common_language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LanguageRow(stringResource(id = R.string.lang_vi), current == "vi") { onChange("vi") }
            LanguageRow(stringResource(id = R.string.lang_ko), current == "ko") { onChange("ko") }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun KioskSection(
    enabled: Boolean,
    hasPin: Boolean,
    isDeviceOwner: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetPin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Chế độ Kiosk", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isDeviceOwner) "Thiết bị đã đăng ký Device Owner — khóa cứng."
                else "Thiết bị chưa đăng ký Device Owner — sẽ dùng Screen Pinning (cần xác nhận trên điện thoại).",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bật chế độ Kiosk", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Khóa thiết bị vào ứng dụng. Thoát bằng mã PIN.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mã PIN quản trị", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hasPin) "Đã đặt PIN. Bấm để đổi." else "Chưa đặt — bắt buộc trước khi bật Kiosk.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = onSetPin) { Text(if (hasPin) "Đổi" else "Đặt") }
            }
        }
    }
}