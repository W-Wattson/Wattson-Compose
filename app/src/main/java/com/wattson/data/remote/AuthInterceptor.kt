package com.wattson.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp Interceptor that automatically injects the Bearer token
 * into all requests except authentication endpoints.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        /** Paths that should NOT receive the Authorization header. */
        private val PUBLIC_AUTH_PATHS = listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestPath = originalRequest.url.encodedPath

        // Skip public auth endpoints — they don't need a Bearer token
        if (PUBLIC_AUTH_PATHS.any { requestPath.endsWith(it) }) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenProvider.getAccessToken()

        return if (accessToken != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
