package com.restaurant.staff.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.restaurant.staff.network.UserSummaryDto
import com.restaurant.staff.repository.AuthRepository
import com.restaurant.staff.storage.KioskSettingsStore
import com.restaurant.staff.storage.SessionStore
import com.restaurant.staff.ui.kiosk.AdminPinDialog
import com.restaurant.staff.ui.theme.RestaurantStaffTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val kioskStore: KioskSettingsStore,
) : ViewModel() {

    data class UiState(
        val user: UserSummaryDto? = null,
        val kioskEnabled: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val u = session.user.first()
            _state.value = UiState(u, kioskStore.currentEnabled())
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            onDone()
        }
    }

    /** Verify PIN and, on success, exit Lock Task and disable the Kiosk flag. */
    suspend fun verifyAndExitKiosk(input: String, kiosk: KioskController): Boolean {
        val ok = kiosk.verifyPin(input)
        if (ok) {
            kioskStore.setEnabled(false)
            val ctx = (kiosk as? Any)?.let { /* no-op */ } as? android.content.Context
            (ctx as? androidx.activity.ComponentActivity)?.let { kiosk.stopLockTask(it) }
        }
        return ok
    }

    /**
     * Helper that uses the Activity reference captured at call time. Avoids
     * holding an Android Context inside the ViewModel (which is generally unsafe).
     */
    fun exitKiosk(
        input: String,
        kiosk: KioskController,
        activity: androidx.activity.ComponentActivity,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val ok = kiosk.verifyPin(input)
            if (ok) {
                kiosk.clearKioskRestrictions()
                kioskStore.setEnabled(false)
                kiosk.stopLockTask(activity)
                onSuccess()
            } else {
                _state.value = _state.value.copy(kioskEnabled = _state.value.kioskEnabled)
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    kiosk: KioskController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val u = state.user
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val scope = rememberCoroutineScope()

    var showExitDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.profile_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                if (u == null) {
                    Text(text = stringResource(id = R.string.splash_loading))
                } else {
                    LabelValue(stringResource(id = R.string.login_username), u.username ?: "—")
                    LabelValue(stringResource(id = R.string.profile_full_name), u.fullName ?: "—")
                    val roleText = when (u.role) {
                        "ADMIN" -> stringResource(id = R.string.profile_role_admin)
                        "STAFF" -> stringResource(id = R.string.profile_role_staff)
                        else -> u.role ?: "—"
                    }
                    LabelValue(stringResource(id = R.string.profile_role_label), roleText)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.common_settings))
                }
                if (state.kioskEnabled) {
                    OutlinedButton(
                        onClick = { showExitDialog = true; pinError = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Thoát chế độ Kiosk") }
                }
                Button(
                    onClick = { viewModel.logout(onLoggedOut) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.profile_logout))
                }
            }
        }
    }

    if (showExitDialog && activity != null) {
        AdminPinDialog(
            title = "Nhập PIN quản trị để thoát Kiosk",
            confirmLabel = "Thoát",
            errorMessage = pinError,
            onConfirm = { pin ->
                viewModel.exitKiosk(
                    input = pin,
                    kiosk = kiosk,
                    activity = activity,
                    onSuccess = { showExitDialog = false }
                )
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}