package com.restaurant.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.restaurant.staff.kiosk.KioskController
import com.restaurant.staff.storage.KioskSettingsStore
import com.restaurant.staff.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var kioskSettings: KioskSettingsStore
    @Inject lateinit var kiosk: KioskController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }

        // PHASE 12: if Kiosk Mode is enabled and we are Device Owner, enter Lock Task
        // automatically on cold start so a power cycle doesn't leave the device exposed.
        lifecycleScope.launch {
            val enabled = kioskSettings.enabled.firstOrNull() ?: false
            if (enabled && kiosk.isDeviceOwner && kiosk.isLockTaskPermitted) {
                kiosk.setLockTaskFeatures(0) // No global actions; PIN-only exit
                kiosk.applyKioskRestrictions()
                kiosk.startLockTask(this@MainActivity)
            }
        }
    }

    override fun onDestroy() {
        // Best-effort: nothing to do here; OS tears down lock task with the activity.
        super.onDestroy()
    }
}