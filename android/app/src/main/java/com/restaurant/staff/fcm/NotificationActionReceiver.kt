package com.restaurant.staff.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log as AndroidLog
import com.restaurant.staff.notifications.NotificationsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.3 / V18 — Handles "Accept" / "Decline" taps on a system notification.
 *
 * The receiver is registered via the manifest with the
 * {@link #ACTION_RESPOND} action. The PendingIntent we built in
 * [RestaurantFcmService.showSystemNotification] carries the notification id
 * plus the chosen verdict. We hand both to the server through
 * [NotificationsRepository.respond].
 *
 * Why a broadcast (not a service call):
 *   - Broadcasts survive the app process being killed; the OS wakes us up
 *     for the few hundred milliseconds needed to POST the verdict.
 *   - The user may have already swiped the app away, or the device may be
 *     in Doze. The system still routes the broadcast.
 *
 * Failure modes are intentionally not surfaced: the worst that happens if
 * the network is down is that the user re-opens the app and sees an
 * unanswered notification with the action buttons still available.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repo: NotificationsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESPOND) return
        val nid = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1L)
        val verdict = intent.getStringExtra(EXTRA_VERDICT) ?: return
        if (nid <= 0) return
        if (verdict != "ACCEPTED" && verdict != "DECLINED") return

        val pending = goAsync()
        scope.launch {
            try {
                val saved = repo.respond(nid, verdict)
                AndroidLog.i(TAG, "respond nid=$nid verdict=$verdict -> saved=$saved")
            } catch (ex: Throwable) {
                AndroidLog.w(TAG, "respond failed: ${ex.javaClass.simpleName}: ${ex.message}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"

        const val ACTION_RESPOND = "com.restaurant.staff.fcm.ACTION_RESPOND"
        const val EXTRA_NOTIFICATION_ID = "com.restaurant.staff.fcm.EXTRA_NOTIFICATION_ID"
        const val EXTRA_VERDICT = "com.restaurant.staff.fcm.EXTRA_VERDICT"
    }
}