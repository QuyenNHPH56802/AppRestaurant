package com.restaurant.staff.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurant.staff.R
import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.storage.ServerConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val configStore: ServerConfigStore,
    private val apiProvider: ApiClientProvider
) : ViewModel() {

    sealed interface Decision {
        data object NeedsPairing : Decision
        data object NeedsLogin : Decision
        data object Error : Decision
    }

    private val _decision = MutableStateFlow<Decision?>(null)
    val decision = _decision.asStateFlow()

    fun decide() {
        viewModelScope.launch {
            val cfg = configStore.current()
            if (cfg == null) {
                _decision.value = Decision.NeedsPairing
                return@launch
            }
            runCatching { apiProvider.serverApiFor(cfg).health() }
                .onSuccess { _decision.value = Decision.NeedsLogin }
                .onFailure { _decision.value = Decision.NeedsPairing }
        }
    }
}

@Composable
fun SplashScreen(
    onNeedsPairing: () -> Unit,
    onNeedsLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.decide() }
    val decision = viewModel.decision
    LaunchedEffect(decision.value) {
        when (decision.value) {
            SplashViewModel.Decision.NeedsPairing -> onNeedsPairing()
            SplashViewModel.Decision.NeedsLogin -> onNeedsLogin()
            SplashViewModel.Decision.Error -> onNeedsPairing()
            null -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(text = stringResource(id = R.string.splash_loading))
        }
    }
}