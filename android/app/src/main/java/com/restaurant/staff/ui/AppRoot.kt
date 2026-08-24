package com.restaurant.staff.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.restaurant.staff.ui.auth.LoginScreen
import com.restaurant.staff.ui.notifications.NotificationPermissionDialog
import com.restaurant.staff.ui.notifications.NotificationsScreen
import com.restaurant.staff.ui.pairing.PairingScreen
import com.restaurant.staff.ui.splash.SplashScreen
import kotlinx.coroutines.flow.MutableSharedFlow

object Routes {
    const val SPLASH = "splash"
    const val PAIRING = "pairing"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME_TAB = "home_tab"
    const val MENU_TAB = "menu_tab"
    const val PROFILE_TAB = "profile_tab"
    const val SETTINGS = "settings"
    const val FOOD_DETAIL = "food/{id}"
    fun foodDetail(id: Long) = "food/$id"

    // V2.3 / V18 — deep-link target for a tapped notification.
    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_DETAIL = "notifications/{id}"
    fun notificationDetail(id: Long) = "notifications/$id"

    // V2.2 — staff-only operational screens.
    const val SHIFTS = "shifts"
    const val ZONES = "zones"
    const val CHECKLISTS = "checklists"
    const val CHECKIN = "checkin"
}

/**
 * V2.3 / V18 — Intent extras read by [com.restaurant.staff.fcm.RestaurantFcmService]
 * to drive the deep-link.
 *
 * They are defined here (not in the fcm package) so the ui layer can consume
 * them without depending on the fcm package directly.
 */
object AppRoot {
    const val EXTRA_FROM_NOTIFICATION = "com.restaurant.staff.FROM_NOTIFICATION"
    const val EXTRA_NOTIFICATION_ID = "com.restaurant.staff.NOTIFICATION_ID"
    const val EXTRA_NOTIFICATION_PAYLOAD = "com.restaurant.staff.NOTIFICATION_PAYLOAD"
}

/**
 * Bus used by [com.restaurant.staff.MainActivity] to forward intent extras to
 * [AppRoot]. Each navigation event is one-shot: collected once, then ignored.
 * Plain SharedFlow keeps the dependency on Hilt-free (the FCM service publishes
 * here, the activity relays).
 */
object DeepLinkBus {
    val events: MutableSharedFlow<DeepLinkEvent> = MutableSharedFlow(extraBufferCapacity = 1)
}

sealed interface DeepLinkEvent {
    data class NotificationDetail(val id: Long, val payloadJson: String?) : DeepLinkEvent
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // V2.3 / V18 — when an Activity arrives with EXTRA_FROM_NOTIFICATION, push
    // the deep-link onto the bus. AppRoot collects it once we're on MAIN and
    // routes to the notification detail.
    LaunchedEffect(Unit) {
        if (context is Activity) {
            val intent = context.intent
            val fromNotification = intent.getBooleanExtra(AppRoot.EXTRA_FROM_NOTIFICATION, false)
            val nid = intent.getLongExtra(AppRoot.EXTRA_NOTIFICATION_ID, -1L)
            val payload = intent.getStringExtra(AppRoot.EXTRA_NOTIFICATION_PAYLOAD)
            if (fromNotification && nid > 0) {
                DeepLinkBus.events.tryEmit(
                    DeepLinkEvent.NotificationDetail(nid, payload)
                )
                // Consume so the same intent doesn't re-fire on rotation.
                intent.removeExtra(AppRoot.EXTRA_FROM_NOTIFICATION)
                intent.removeExtra(AppRoot.EXTRA_NOTIFICATION_ID)
                intent.removeExtra(AppRoot.EXTRA_NOTIFICATION_PAYLOAD)
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNeedsPairing = {
                    navController.navigate(Routes.PAIRING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNeedsLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PAIRING) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScaffold(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onOpenPairing = {
                    navController.navigate(Routes.PAIRING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                deepLink = DeepLinkBus.events,
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenNotificationDetail = { id -> navController.navigate(Routes.notificationDetail(id)) }
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenNotification = { id -> navController.navigate(Routes.notificationDetail(id)) }
            )
        }
        composable(Routes.NOTIFICATION_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            if (id == null) {
                // Bad id; pop back.
                navController.popBackStack()
            } else {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNotification = { /* no-op when on detail */ },
                    openNotificationId = id
                )
            }
        }
        // V2.2 — operational screens. Reachable from Profile via deep links
        // for now; future story will surface them in the bottom-nav bar.
        composable(Routes.SHIFTS) {
            com.restaurant.staff.ui.shifts.ShiftsScreen()
        }
        composable(Routes.ZONES) {
            com.restaurant.staff.ui.zones.ZonesScreen()
        }
        composable(Routes.CHECKLISTS) {
            com.restaurant.staff.ui.checklists.ChecklistsScreen()
        }
        composable(Routes.CHECKIN) {
            com.restaurant.staff.ui.checkin.CheckInScreen()
        }
    }

    // V2.3 / V18 — The permission rationale lives at the root so it survives
    // navigation between tabs. The dialog itself decides visibility based on
    // OS version, current permission state, and a one-shot DataStore flag.
    NotificationPermissionDialog()
}