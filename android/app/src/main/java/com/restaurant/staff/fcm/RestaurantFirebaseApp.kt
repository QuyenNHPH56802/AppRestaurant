package com.restaurant.staff.fcm

import android.app.Application
import android.util.Log as AndroidLog
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.restaurant.staff.BuildConfig

/**
 * V2.3 / V18 — Decides whether FCM is wired up at runtime.
 *
 * Unlike google-services.json, we don't ship a hard-coded Firebase config in
 * the repo. Instead {@code local.properties} (git-ignored) carries the four
 * values needed to construct a {@link FirebaseOptions}, and they are exposed
 * via {@code BuildConfig} at build time. If any one is missing the app still
 * boots; {@link #isConfigured} returns false and {@link #init} is a no-op.
 * That keeps dev/test cycles painless on machines without a Firebase project
 * and matches the server's {@code restaurant.fcm.enabled=false} default.
 *
 * Init order: this MUST be called BEFORE any Firebase SDK touches the app
 * context. We hook it from {@code RestaurantStaffApp.onCreate()} right after
 * Hilt graph init.
 */
object RestaurantFirebaseApp {

    private const val TAG = "RestaurantFirebaseApp"

    /** True when BuildConfig has all four FCM fields populated. */
    val isConfigured: Boolean
        get() = BuildConfig.FCM_PROJECT_ID.isNotBlank()
             && BuildConfig.FCM_APP_ID.isNotBlank()
             && BuildConfig.FCM_API_KEY.isNotBlank()
             && BuildConfig.FCM_SENDER_ID.isNotBlank()

    /**
     * Initialise the SDK. Idempotent: a second call with FirebaseApp already
     * set up returns the existing instance. Returns null when FCM is not
     * configured — callers must handle that branch (the FcmService does, by
     * short-circuiting its onMessageReceived callback).
     */
    fun init(application: Application): FirebaseApp? {
        if (!isConfigured) {
            AndroidLog.i(TAG, "FCM not configured (BuildConfig FCM_* blank); push disabled")
            return null
        }
        if (FirebaseApp.getApps(application).isNotEmpty()) {
            return FirebaseApp.getInstance()
        }
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .setApplicationId(BuildConfig.FCM_APP_ID)
                .setApiKey(BuildConfig.FCM_API_KEY)
                .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(application, options, FirebaseApp.DEFAULT_APP_NAME)
        } catch (ex: Throwable) {
            // We never want a Firebase init failure to crash the app. The in-app
            // REST feed is still usable, and an admin can later edit
            // local.properties and reinstall.
            AndroidLog.w(TAG, "FirebaseApp.initializeApp failed: ${ex.javaClass.simpleName}: ${ex.message}")
            null
        }
    }
}