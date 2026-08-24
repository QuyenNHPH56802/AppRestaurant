package com.restaurant.staff.ui.notifications

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.restaurant.staff.R
import com.restaurant.staff.fcm.NotificationPermissionState
import com.restaurant.staff.fcm.NotificationPrefsStore
import com.restaurant.staff.fcm.NotificationPermissionState.PERMISSION_NAME
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Hilt entry-point shim — we need [NotificationPrefsStore] at the Compose root
 * without dragging Hilt into a Composable directly.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationPermissionEntryPoint {
    fun notificationPrefsStore(): NotificationPrefsStore
}

/**
 * V2.3 / V18 — In-app rationale dialog for {@code POST_NOTIFICATIONS}.
 *
 * The OS only shows the system dialog when an app first calls
 * {@code ActivityResultContracts.RequestPermission}. Our app never posts
 * a notification without first explaining why (this dialog), and we cap
 * to one ask per install / per user because:
 *   - after the user taps "Skip" once we don't badger them again
 *   - the system dialog itself is single-shot: tapping "Deny" twice with
 *     "don't ask again" (only available on Settings screen) blocks us for
 *     good until they re-enable manually
 *
 * The dialog is therefore:
 *   - invisible on API < 33
 *   - invisible when permission is already GRANTED
 *   - invisible when [NotificationPrefsStore.prompted] is true
 *   - visible once on the rest
 */
@Composable
fun NotificationPermissionDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, NotificationPermissionEntryPoint::class.java
        ).notificationPrefsStore()
    }
    val prompted by prefs.prompted.collectAsState(initial = true)

    // If we're on a modern OS, currently denied, and haven't asked yet, show
    // the dialog. The OS dialog is triggered by the launcher we register
    // below; once the user makes a choice (grant or deny) we mark prompted
    // so we never auto-pop this in-app rationale again.
    val shouldShow = remember(prompted) {
        NotificationPermissionState.current(context) == NotificationPermissionState.State.DENIED
                && !prompted
    }

    // The system launcher must live at the Composable root so its result
    // callback survives recomposition. It doesn't auto-show; we kick it
    // from a side-effect once the dialog's "allow" button is tapped.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted or not, we don't want to bug the user again.
        scope.launch { prefs.setPrompted(true) }
    }

    var visible by remember { mutableStateOf(shouldShow) }
    LaunchedEffect(shouldShow) { visible = shouldShow }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            // Tap outside / back: dismiss for this session but don't mark
            // prompted; the user can re-trigger from Settings later.
            visible = false
        },
        title = { Text(stringResource(R.string.notifications_permission_title)) },
        text = { Text(stringResource(R.string.notifications_permission_request)) },
        confirmButton = {
            TextButton(onClick = {
                visible = false
                scope.launch { prefs.setPrompted(true) }
                launcher.launch(PERMISSION_NAME)
            }) {
                Text(stringResource(R.string.notifications_permission_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                visible = false
                scope.launch { prefs.setPrompted(true) }
            }) {
                Text(stringResource(R.string.notifications_permission_skip))
            }
        }
    )
}