package com.restaurant.staff.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restaurant.staff.R
import com.restaurant.staff.repository.AuthRepository
import com.restaurant.staff.ui.theme.RestaurantStaffTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    data class UiState(
        val username: String = "",
        val password: String = "",
        val loading: Boolean = false,
        val errorKey: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun onUsername(value: String) = _state.update { it.copy(username = value, errorKey = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, errorKey = null) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(errorKey = "EMPTY_FIELDS") }
            return
        }
        _state.update { it.copy(loading = true, errorKey = null) }
        viewModelScope.launch {
            runCatching { auth.login(s.username.trim(), s.password) }
                .fold(
                    onSuccess = {
                        _state.update { it.copy(loading = false) }
                        onSuccess()
                    },
                    onFailure = { ex ->
                        val key = when (ex) {
                            is com.restaurant.staff.repository.AuthException -> ex.code
                            else -> "NETWORK"
                        }
                        _state.update { it.copy(loading = false, errorKey = key) }
                    }
                )
        }
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(id = R.string.login_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsername,
                    label = { Text(stringResource(id = R.string.login_username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    label = { Text(stringResource(id = R.string.login_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.submit(onLoggedIn) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(Modifier.height(0.dp))
                    } else {
                        Text(stringResource(id = R.string.login_button))
                    }
                }

                Spacer(Modifier.height(12.dp))
                when (state.errorKey) {
                    "INVALID_CREDENTIALS", "UNAUTHORIZED" ->
                        Text(stringResource(id = R.string.login_invalid_credentials),
                            color = MaterialTheme.colorScheme.error)
                    "NETWORK" ->
                        Text(stringResource(id = R.string.login_error_network),
                            color = MaterialTheme.colorScheme.error)
                    "EMPTY_FIELDS" ->
                        Text(stringResource(id = R.string.login_invalid_credentials),
                            color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}