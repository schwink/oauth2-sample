package com.example.oauth2sample.app.auth

import net.openid.appauth.AuthState

data class UserSession(
    private val service: UserSessionService,
    internal val authState: AuthState,
    val username: String?,
    val email: String?,
) {

    suspend fun <T> withTokens(action: (authToken: String, idToken: String) -> T): T =
        service.withTokens(authState, action)
}
