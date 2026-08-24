package com.restaurant.staff.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * V2.3 / V18 — Constants for FCM wiring on the device.
 *
 * Single place that owns channel IDs + importance levels so the manifest,
 * the [com.restaurant.staff.fcm.RestaurantFcmService] renderer, and the
 * settings UI all stay in sync.
 *
 * Channel strategy:
 *   - {@link #CHANNEL_ID_DEFAULT}  — fallback for untyped push (system, ops)
 *   - {@link #CHANNEL_ID_SHIFT}    — shift assignments, schedule changes (HIGH)
 *   - {@link #CHANNEL_ID_ZONE}     — zone changes, table moves (HIGH)
 *   - {@link #CHANNEL_ID_MANAGER}  — direct messages from a manager (HIGH)
 *
 * Why multiple channels: Android 8+ requires a channel for every notification
 * and lets the user mute one category independently. Shift alerts must wake
 * the device; manager messages may be muted; system alerts may be muted too.
 */
object NotificationChannels {

    const val CHANNEL_ID_DEFAULT = "fcm_default"
    const val CHANNEL_ID_SHIFT   = "fcm_shift"
    const val CHANNEL_ID_ZONE    = "fcm_zone"
    const val CHANNEL_ID_MANAGER = "fcm_manager"

    /**
     * Map a server-side notification type to a channel id. Unknown types fall
     * through to the default channel so we never silently drop a notification.
     */
    fun channelIdForType(type: String?): String = when (type) {
        "SHIFT_ASSIGNED", "SHIFT_CHANGED", "SHIFT_CANCELLED", "SCHEDULE_PUBLISHED" -> CHANNEL_ID_SHIFT
        "ZONE_CHANGED", "ZONE_ASSIGNED"                                            -> CHANNEL_ID_ZONE
        "MANAGER_MESSAGE"                                                          -> CHANNEL_ID_MANAGER
        "SYSTEM_ALERT"                                                             -> CHANNEL_ID_DEFAULT
        else                                                                       -> CHANNEL_ID_DEFAULT
    }

    /**
     * Definition to register at app startup (or after OS upgrade) so we have
     * all channels installed before the first push arrives. {@code
     * NotificationManager.createNotificationChannel} is idempotent: existing
     * channels are not overwritten, only added when missing.
     */
    fun definitions(): List<NotificationChannel> = listOf(
        buildChannel(
            CHANNEL_ID_DEFAULT,
            nameRes = "Thông báo",
            descRes = "Thông báo chung từ hệ thống",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ),
        buildChannel(
            CHANNEL_ID_SHIFT,
            nameRes = "Phân ca / Lịch làm việc",
            descRes = "Thông báo khi bạn được phân ca, đổi ca, hoặc lịch tuần mới được công bố.",
            importance = NotificationManager.IMPORTANCE_HIGH
        ),
        buildChannel(
            CHANNEL_ID_ZONE,
            nameRes = "Khu vực / Bàn",
            descRes = "Thông báo khi bạn được phân công sang khu vực hoặc bàn khác.",
            importance = NotificationManager.IMPORTANCE_HIGH
        ),
        buildChannel(
            CHANNEL_ID_MANAGER,
            nameRes = "Tin nhắn từ quản lý",
            descRes = "Tin nhắn riêng từ quản lý nhà hàng.",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    )

    private fun buildChannel(
        id: String,
        nameRes: String,
        descRes: String,
        importance: Int
    ): NotificationChannel {
        // API 26+ for NotificationChannel. On older devices the channels are
        // implicitly created by the system when notify() is first called;
        // we still hand back the same object so callers don't need to branch.
        val ch = NotificationChannel(id, nameRes, importance)
        ch.description = descRes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ch.enableVibration(true)
            ch.setShowBadge(true)
        }
        return ch
    }
}