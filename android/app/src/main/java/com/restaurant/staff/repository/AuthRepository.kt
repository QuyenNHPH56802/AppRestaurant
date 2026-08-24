package com.restaurant.staff.repository

import com.restaurant.staff.fcm.NotificationPrefsStore
import com.restaurant.staff.fcm.TokenRotator
import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.network.LoginRequest
import com.restaurant.staff.network.LoginResponseDto
import com.restaurant.staff.network.UserSummaryDto
import com.restaurant.staff.notifications.UnreadBadgeHolder
import com.restaurant.staff.storage.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiClientProvider,
    private val session: SessionStore,
    private val tokenRotator: TokenRotator,
    private val notificationPrefs: NotificationPrefsStore,
    private val badge: UnreadBadgeHolder
) {
    suspend fun login(username: String, password: String): UserSummaryDto {
        val resp = api.authApi().login(LoginRequest(username, password))
        if (resp.success != true || resp.data == null || resp.data.token.isNullOrBlank()) {
            throw AuthException(resp.error?.code ?: "UNKNOWN", resp.error?.message ?: "Login failed")
        }
        val data: LoginResponseDto = resp.data
        val user = data.user ?: throw AuthException("NO_USER", "Server did not return user")
        session.save(data.token, user)
        // V2.3 / V18 — register the device for push right after login so the
        // server can dispatch to this token from the first shift-assigned push.
        // Best-effort: failure must not roll back the login.
        tokenRotator.registerAfterLogin()
        // V2.3 / V18 — start polling the unread badge so the bottom-bar tab
        // shows a count the moment a push is delivered.
        badge.start()
        // V2.3 / V18 — clear the "we've already prompted" flag so the next
        // user on the same device gets a fresh chance to opt-in.
        runCatching { notificationPrefs.reset() }
        return user
    }

    suspend fun logout() {
        val token = session.currentToken() ?: return
        runCatching { api.authApi().logout("Bearer $token") }
        // V2.3 / V18 — stop the badge polling so a stale count can't bleed
        // across to the next login on the same device.
        badge.stop()
        // V2.3 / V18 — best-effort deactivate the device token so a future
        // login on a DIFFERENT device doesn't accidentally deliver here.
        runCatching { tokenRotator.unregisterOnLogout() }
        session.clear()
    }
}

class AuthException(val code: String, message: String) : RuntimeException(message)