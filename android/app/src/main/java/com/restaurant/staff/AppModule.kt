package com.restaurant.staff

import android.content.Context
import com.restaurant.staff.fcm.NotificationPrefsStore
import com.restaurant.staff.fcm.TokenStore
import com.restaurant.staff.kiosk.KioskController
import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.network.V22Api
import com.restaurant.staff.notifications.NotificationsDataSource
import com.restaurant.staff.notifications.NotificationsDataSourceImpl
import com.restaurant.staff.notifications.NotificationsRepository
import com.restaurant.staff.repository.V22Repository
import com.restaurant.staff.storage.KioskSettingsStore
import com.restaurant.staff.storage.LocaleStore
import com.restaurant.staff.storage.ServerConfigStore
import com.restaurant.staff.storage.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideServerConfigStore(@ApplicationContext context: Context): ServerConfigStore =
        ServerConfigStore(context)

    @Provides
    @Singleton
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore =
        SessionStore(context)

    @Provides
    @Singleton
    fun provideApiClientProvider(
        configStore: ServerConfigStore,
        sessionStore: SessionStore
    ): ApiClientProvider = ApiClientProvider(configStore, sessionStore, debug = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideLocaleStore(@ApplicationContext context: Context): LocaleStore =
        LocaleStore(context)

    @Provides
    @Singleton
    fun provideKioskSettingsStore(@ApplicationContext context: Context): KioskSettingsStore =
        KioskSettingsStore(context)

    @Provides
    @Singleton
    fun provideKioskController(
        @ApplicationContext context: Context,
        store: KioskSettingsStore
    ): KioskController = KioskController(context, store)

    // V2.3 / V18 — DataStore-backed holder for the current FCM token. The
    // TokenRotator reads/writes here to detect rotation and survive logout.
    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        TokenStore(context)

    // V2.3 / V18 — Tracks whether we've already shown the runtime permission
    // banner so we don't prompt the same user twice in one app lifetime.
    @Provides
    @Singleton
    fun provideNotificationPrefsStore(@ApplicationContext context: Context): NotificationPrefsStore =
        NotificationPrefsStore(context)

    // V2.2 — expose the V22 Retrofit interface directly so ViewModels can
    // pick it up via constructor injection without going through the
    // repository. The repository also uses this same singleton.
    @Provides
    @Singleton
    fun provideV22Api(provider: ApiClientProvider): V22Api = provider.v22Api()

    @Provides
    @Singleton
    fun provideV22Repository(api: V22Api): V22Repository = V22Repository(api)

    // V2.3 / V18 — Phase F — Wire the notifications data-source seam so the
    // ViewModel can be unit-tested with a hand-rolled fake.
    @Provides
    @Singleton
    fun provideNotificationsDataSource(repo: NotificationsRepository): NotificationsDataSource =
        NotificationsDataSourceImpl(repo)
}