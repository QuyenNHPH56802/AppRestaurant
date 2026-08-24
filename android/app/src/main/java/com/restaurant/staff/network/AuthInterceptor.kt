package com.restaurant.staff.network

import com.restaurant.staff.storage.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to every request that targets a
 * protected endpoint. The login/health/server-info endpoints are public and
 * the server doesn't require a token there.
 */
class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {

    private val publicPaths = setOf(
        "/api/auth/login",
        "/api/health",
        "/api/server/info"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (publicPaths.any { path == it || path.startsWith("$it/") }) {
            return chain.proceed(request)
        }
        val token = runBlocking { sessionStore.currentToken() }
        val authed = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(authed)
    }
}