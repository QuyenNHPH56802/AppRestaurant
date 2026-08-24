package com.restaurant.staff.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.restaurant.staff.storage.LocaleStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user-selected locale to the entire app via AppCompatDelegate.
 * Should be installed once at app startup.
 */
@Singleton
class LocaleApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocaleStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun install() {
        scope.launch {
            store.language.collectLatest { lang ->
                val tag = if (lang == "ko") "ko" else "vi"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }
}