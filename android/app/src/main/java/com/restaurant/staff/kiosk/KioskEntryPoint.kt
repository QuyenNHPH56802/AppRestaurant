package com.restaurant.staff.kiosk

import com.restaurant.staff.storage.KioskSettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * PHASE 12: Hilt EntryPoint used by non-injected Composables (like SettingsScreen
 * which is currently Hilt-injected only at the ViewModel layer) to retrieve
 * the singleton [KioskController].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface KioskEntryPoint {
    fun kioskController(): KioskController
    fun kioskStore(): KioskSettingsStore
}