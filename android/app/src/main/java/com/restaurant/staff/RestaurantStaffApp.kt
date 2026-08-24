package com.restaurant.staff

import android.app.Application
import android.util.Log as AndroidLog
import com.restaurant.staff.fcm.RestaurantFirebaseApp
import com.restaurant.staff.fcm.NotificationChannels
import com.restaurant.staff.i18n.LocaleApplier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry. Installs the per-app locale (vi / ko) at startup, so the
 * Activity reads string resources in the user's preferred language. Also wires
 * FCM (Phase V2.3) when BuildConfig has Firebase credentials, and ensures
 * the notification channels exist before the first push arrives.
 */
@HiltAndroidApp
class RestaurantStaffApp : Application() {

    @Inject lateinit var localeApplier: LocaleApplier

    override fun onCreate() {
        super.onCreate()
        localeApplier.install()

        // V2.3 / V18 — FCM init. Returns null when BuildConfig is blank, so
        // a build without google-services.json still boots cleanly.
        val app = RestaurantFirebaseApp.init(this)
        if (app == null) {
            AndroidLog.i("RestaurantStaffApp", "FCM disabled (BuildConfig blank or init failed)")
        }

        // Ensure notification channels exist (idempotent on API 26+).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            NotificationChannels.definitions().forEach { ch ->
                if (nm.getNotificationChannel(ch.id) == null) {
                    nm.createNotificationChannel(ch)
                }
            }
        }
    }
}