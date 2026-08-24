package com.restaurant.staff.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.restaurant.staff.R
import com.restaurant.staff.kiosk.KioskController
import com.restaurant.staff.kiosk.KioskEntryPoint
import com.restaurant.staff.notifications.UnreadBadgeHolder
import com.restaurant.staff.ui.food.FoodDetailScreen
import com.restaurant.staff.ui.home.HomeScreen
import com.restaurant.staff.ui.menu.MenuScreen
import com.restaurant.staff.ui.profile.ProfileScreen
import com.restaurant.staff.ui.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Hilt entry-point shim — UI pulls [UnreadBadgeHolder] without dragging Hilt
 * into Composable. Matches the pattern used in [com.restaurant.staff.ui.notifications.NotificationPermissionDialog].
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface MainEntryPoint {
    fun unreadBadgeHolder(): UnreadBadgeHolder
}

private data class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun MainScaffold(
    onLoggedOut: () -> Unit,
    onOpenPairing: () -> Unit,
    deepLink: MutableSharedFlow<DeepLinkEvent>,
    onOpenNotifications: () -> Unit,
    onOpenNotificationDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val kiosk: KioskController = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, KioskEntryPoint::class.java
        ).kioskController()
    }
    // V2.3 / V18 — pull the process-singleton badge holder. It polls every
    // 30s while the user is logged in; AuthRepository starts/stops it on
    // login/logout, so by the time MainScaffold exists, the loop is running.
    val badge = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext, MainEntryPoint::class.java
        ).unreadBadgeHolder()
    }
    val unread by badge.count.collectAsState()

    val nav: NavHostController = rememberNavController()
    val items = listOf(
        BottomItem(Routes.HOME_TAB, R.string.home_featured) { Icon(Icons.Filled.Home, contentDescription = null) },
        BottomItem(Routes.MENU_TAB, R.string.menu_title) { Icon(Icons.Filled.Restaurant, contentDescription = null) },
        BottomItem(Routes.PROFILE_TAB, R.string.profile_title) { Icon(Icons.Filled.Person, contentDescription = null) }
    )

    // V2.3 / V18 — collect deep-link events arriving from a tapped notification.
    // The bus is one-shot (SharedFlow without replay); the same event will not
    // re-fire on rotation because Activity removes the extras after publishing.
    LaunchedEffect(Unit) {
        deepLink.collect { event ->
            when (event) {
                is DeepLinkEvent.NotificationDetail -> onOpenNotificationDetail(event.id)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val entry by nav.currentBackStackEntryAsState()
                val currentRoute = entry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(stringResource(id = item.labelRes)) }
                    )
                }
                // V2.3 / V18 — Notifications tab with a live badge. The
                // BadgedBox wrapper keeps the icon position stable whether the
                // count is 0 or >0; only the small bubble animates in/out.
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenNotifications,
                    icon = {
                        BadgedBox(badge = {
                            if (unread > 0L) {
                                // Compose's Badge caps the visible label at
                                // "99+" automatically past 99. We pass Long to
                                // honor the server's wider count window.
                                Badge { Text(unread.coerceAtMost(99).toString()) }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(R.string.notifications_title)) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME_TAB,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME_TAB) {
                HomeScreen(onFoodClick = { id -> nav.navigate(Routes.foodDetail(id)) })
            }
            composable(Routes.MENU_TAB) {
                MenuScreen(onFoodClick = { id -> nav.navigate(Routes.foodDetail(id)) })
            }
            composable(Routes.PROFILE_TAB) {
                ProfileScreen(
                    onLoggedOut = onLoggedOut,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    kiosk = kiosk
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenPairing = onOpenPairing,
                    onOpenShifts = { nav.navigate(Routes.SHIFTS) },
                    onOpenZones = { nav.navigate(Routes.ZONES) },
                    onOpenChecklists = { nav.navigate(Routes.CHECKLISTS) },
                    onOpenCheckIn = { nav.navigate(Routes.CHECKIN) },
                    kiosk = kiosk
                )
            }
            composable(Routes.FOOD_DETAIL) {
                FoodDetailScreen()
            }
        }
    }
}