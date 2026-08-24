package com.restaurant.staff.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restaurant.staff.R
import com.restaurant.staff.ui.theme.RestaurantStaffTheme

@Composable
fun PairingScreen(
    onPaired: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RestaurantStaffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = stringResource(id = R.string.pairing_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(id = R.string.pairing_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (state.savedConfig != null) {
                    SavedServerCard(
                        host = state.savedConfig!!.host,
                        port = state.savedConfig!!.port,
                        version = state.savedConfig!!.serverVersion,
                        onClear = viewModel::clearSavedConfig
                    )
                }

                Button(
                    onClick = viewModel::startScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.pairing_scan_qr))
                }

                HorizontalDivider()

                Text(
                    text = stringResource(id = R.string.pairing_manual_entry),
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = state.manualHost,
                    onValueChange = viewModel::updateManualHost,
                    label = { Text(stringResource(id = R.string.pairing_server_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.manualPort,
                    onValueChange = viewModel::updateManualPort,
                    label = { Text(stringResource(id = R.string.pairing_port_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.connectManually(onPaired) },
                    enabled = !state.testing && state.manualHost.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.testing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(id = R.string.pairing_connect))
                }

                when (val r = state.testResult) {
                    is PairingViewModel.TestResult.Success -> {
                        Text(
                            stringResource(id = R.string.pairing_test_success),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    is PairingViewModel.TestResult.Failed -> {
                        Text(
                            stringResource(id = R.string.pairing_test_failed) + " (${r.reason})",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    null -> Unit
                }
            }
        }
    }

    if (state.scanning) {
        QrScanScreen(
            onText = { text -> viewModel.onQrText(text, onPaired) },
            onCancel = viewModel::stopScan
        )
    }
}

@Composable
private fun SavedServerCard(host: String, port: Int, version: String?, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(id = R.string.pairing_settings_server), style = MaterialTheme.typography.titleSmall)
            Text(text = "$host:$port", style = MaterialTheme.typography.bodyMedium)
            if (version != null) {
                Text(text = "v$version", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onClear) {
            Text(stringResource(id = R.string.common_settings))
        }
    }
}