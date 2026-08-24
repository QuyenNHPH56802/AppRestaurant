package com.restaurant.staff.i18n

import com.restaurant.staff.storage.LocaleStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes the active language as a StateFlow. ViewModels collect this and
 * pass it to repositories when building API requests.
 */
@Singleton
class LocaleHolder @Inject constructor(
    private val store: LocaleStore
) {
    fun language(scope: CoroutineScope) = store.language.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = "vi"
    )
}