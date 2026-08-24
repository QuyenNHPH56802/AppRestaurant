package com.restaurant.staff.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log as AndroidLog
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.restaurant.staff.MainActivity
import com.restaurant.staff.storage.LocaleStore
import com.restaurant.staff.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2.3 / V18 — Receives FCM pushes and renders them as a system notification.
 *
 * Lifecycle:
 *   1. The OS calls {@link #onNewToken} when (re)installing the app, or when
 *      the FCM registration rotates for any reason. We hand the new token to
 *      [com.restaurant.staff.fcm.TokenRotator] which is responsible for
 *      registering it with the server.
 *   2. The OS calls {@link #onMessageReceived} for each data push. We build
 *      a system notification in the correct channel (per
 *      [NotificationChannels.channelIdForType]), using the current user's
 *      locale so a vi user sees "Phân ca mới" and a ko user sees
 *      "새 근무 배정".
 *   3. Tapping the notification opens [MainActivity] with an intent extra
 *      carrying the server-side notification id. AppRoot reads it and
 *      navigates to the detail screen.
 *
 * Security: tokens from {@code data} payloads are NEVER written to logs.
 */
@AndroidEntryPoint
class RestaurantFcmService : FirebaseMessagingService() {

    @Inject lateinit var localeStore: LocaleStore
    @Inject lateinit var tokenRotator: TokenRotator

    /** Service-lifetime coroutine scope for the disk-write to DataStore. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AndroidLog.i(TAG, "FCM token rotated (length=${token.length})")
        // Save to local DataStore and post to server (idempotent).
        scope.launch {
            runCatching { TokenStoreExt.put(applicationContext, token) }
            tokenRotator.onRotation(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: "SYSTEM_ALERT"
        val nid = data["nid"]?.toLongOrNull()
        AndroidLog.i(TAG, "push received: type=$type nid=$nid keys=${data.keys}")

        // Resolve title/body in the user's current locale. Server already
        // pre-renders both vi + ko in the data payload under data.title_vi /
        // data.title_ko (see NotificationService.putData("payload")). When
        // the server hasn't pre-rendered (older versions) we fall back to
        // FCM's own notification body so something is still shown.
        val lang = runCatching { localeStore.language.first() }.getOrDefault("vi")
        val title = data["title_$lang"] ?: data["title_vi"]
            ?: message.notification?.title
            ?: "Thông báo"
        val body = data["body_$lang"] ?: data["body_vi"]
            ?: message.notification?.body
            ?: ""

        showSystemNotification(
            type = type,
            title = title,
            body = body,
            nid = nid,
            payloadJson = data["payload"]
        )
    }

    private fun showSystemNotification(
        type: String,
        title: String,
        body: String,
        nid: Long?,
        payloadJson: String?
    ) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = NotificationChannels.channelIdForType(type)

        // Tap intent → MainActivity, with extras AppRoot reads to navigate.
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // We piggy-back the deep-link payload as a single string so AppRoot
            // doesn't need separate extras for nid + payload.
            putExtra(AppRoot.EXTRA_FROM_NOTIFICATION, true)
            if (nid != null) putExtra(AppRoot.EXTRA_NOTIFICATION_ID, nid)
            if (payloadJson != null) putExtra(AppRoot.EXTRA_NOTIFICATION_PAYLOAD, payloadJson)
        }
        val pi = PendingIntent.getActivity(
            this,
            (nid ?: 0).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(if (type == "MANAGER_MESSAGE")
                NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_EVENT)
            .setPriority(
                if (channelId == NotificationChannels.CHANNEL_ID_DEFAULT)
                    NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )

        // V2.3 / V18 — For respondable types we attach Accept / Decline
        // actions that broadcast to a NotificationActionReceiver. We use a
        // broadcast (rather than a direct service call) so the action survives
        // the app being killed and the OS only needs to wake us briefly to
        // POST the verdict. The receiver is in the fcm package.
        if (nid != null && type in RespondablePushTypes) {
            builder.addAction(
                0,
                getString(com.restaurant.staff.R.string.notification_action_accept),
                buildActionPendingIntent(nid, "ACCEPTED")
            )
            builder.addAction(
                0,
                getString(com.restaurant.staff.R.string.notification_action_decline),
                buildActionPendingIntent(nid, "DECLINED")
            )
        }

        // Notification id is derived from nid so two notifications get distinct
        // system rows but a fresh push for the same nid replaces the old one.
        val sysId = nid?.toInt() ?: System.currentTimeMillis().toInt()

        // Notification posting needs POST_NOTIFICATIONS on Android 13+ (Tiramisu).
        // We already added it to the manifest; the runtime grant is asked by
        // the UI layer when the user opens the app for the first time.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || areNotificationsEnabled(nm)) {
            nm.notify(sysId, builder.build())
        } else {
            AndroidLog.w(TAG, "POST_NOTIFICATIONS not granted; dropping push nid=$nid")
        }
    }

    /** Build a PendingIntent that targets [NotificationActionReceiver]. */
    private fun buildActionPendingIntent(nid: Long, verdict: String): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_RESPOND
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, nid)
            putExtra(NotificationActionReceiver.EXTRA_VERDICT, verdict)
        }
        // Different request codes for Accept vs Decline so the OS doesn't
        // reuse the same PendingIntent for both actions.
        return PendingIntent.getBroadcast(
            this,
            (nid * 10 + verdict.hashCode()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun areNotificationsEnabled(nm: NotificationManager): Boolean {
        return nm.areNotificationsEnabled()
    }

    companion object {
        private const val TAG = "RestaurantFcmService"

        /** Push types that surface Accept / Decline action buttons. */
        private val RespondablePushTypes = setOf(
            "SHIFT_ASSIGNED",
            "SHIFT_SWAP_REQUESTED"
        )
    }
}