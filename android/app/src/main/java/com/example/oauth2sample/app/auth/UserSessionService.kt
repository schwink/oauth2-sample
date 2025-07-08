package com.example.oauth2sample.app.auth

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.Preconditions
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.openid.appauth.connectivity.ConnectionBuilder
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "authState")
private val AUTH_STATE_PREFERENCES_KEY = stringPreferencesKey("authState")

internal class AuthStateStore(
    private val context: Context,
) {

    val state: Flow<AuthState?> = context.authDataStore.data.map { preferences ->
        val json = preferences[AUTH_STATE_PREFERENCES_KEY]
        if (json == null) {
            null
        } else {
            try {
                AuthState.jsonDeserialize(json)
            } catch (_: JSONException) {
                null
            }
        }
    }

    suspend fun update(authState: AuthState?) {
        context.authDataStore.updateData { preferences ->
            val mutablePreferences = preferences.toMutablePreferences()
            if (authState == null) {
                mutablePreferences.clear()
            } else {
                val json = authState.jsonSerializeString()
                mutablePreferences[AUTH_STATE_PREFERENCES_KEY] = json
            }

            mutablePreferences
        }
    }
}

/**
 * Note that [AuthState] is a mutable object, which we are sharing instances of via the [StateFlow]
 * in [AuthStateStore].
 *
 * This helper method offers a way to update an AuthState without mutating the original.
 */
fun AuthState.copyUpdate(mutation: (authState: AuthState) -> Unit): AuthState {
    val serialized = this.jsonSerializeString()
    val copy = AuthState.jsonDeserialize(serialized)
    mutation(copy)
    return copy
}

class UserSessionService(
    context: Context,
    private val coroutineScope: CoroutineScope,
) {
    internal val authStateStore: AuthStateStore = AuthStateStore(
        context = context,
    )

    private val insecureAppAuthConfig: AppAuthConfiguration = AppAuthConfiguration.Builder()
        /** A copy of [net.openid.appauth.connectivity.DefaultConnectionBuilder] that bypasses the https:// check */
        .setConnectionBuilder(object : ConnectionBuilder {
            override fun openConnection(uri: Uri): HttpURLConnection {
                Preconditions.checkNotNull<Uri?>(uri, "url must not be null")
                val conn = URL(uri.toString()).openConnection() as HttpURLConnection
                conn.setConnectTimeout(TimeUnit.SECONDS.toMillis(15).toInt())
                conn.setReadTimeout(TimeUnit.SECONDS.toMillis(10).toInt())
                conn.setInstanceFollowRedirects(false)
                return conn
            }
        })
        .setSkipIssuerHttpsCheck(true)
        .build()
    internal val service: AuthorizationService =
        AuthorizationService(context, insecureAppAuthConfig)

    sealed class LoginState {
        /** Waiting for initial load from the auth store */
        data object Loading : LoginState()

        /** The user is not logged in */
        data object LoggedOut : LoginState()

        data class LoggedIn(
            val userSession: UserSession,
        ) : LoginState()
    }

    private val _state = MutableStateFlow<LoginState>(LoginState.Loading)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            val initialAuthState = authStateStore.state.first()
            val initialServiceConfiguration =
                initialAuthState?.authorizationServiceConfiguration

            if (initialServiceConfiguration == null) {
                val serviceConfiguration = fetchServiceConfiguration()

                if (initialAuthState == null) {
                    // Write an empty state with the configuration into the store, to persist it
                    authStateStore.update(AuthState(serviceConfiguration))
                }

                serviceConfiguration
            }

            authStateStore.state.map { authState ->
                if (authState == null) {
                    LoginState.LoggedOut
                } else if (authState.isAuthorized == true) {
                    val claims =
                        authState.parsedIdToken?.additionalClaims ?: mapOf<String, Any>()
                    val email = claims["email"] as String?
                    val username = claims["name"] as String?

                    val userSession = UserSession(
                        service = this@UserSessionService,
                        authState = authState,
                        username = username,
                        email = email
                    )
                    LoginState.LoggedIn(
                        userSession = userSession
                    )
                } else {
                    LoginState.LoggedOut
                }
            }.collect { _state.emit(it) }
        }
    }

    internal suspend fun serviceConfiguration(): AuthorizationServiceConfiguration {
        return authStateStore.state.first()?.authorizationServiceConfiguration
            ?: fetchServiceConfiguration()
    }

    // TODO: Dynamic registration instead of client secret
    // https://github.com/openid/AppAuth-Android?tab=readme-ov-file#utilizing-client-secrets-dangerous
    private val clientAuthentication: ClientAuthentication =
        ClientSecretBasic("b2F1dGgyLXByb3h5LWNsaWVudC1zZWNyZXQK")

    private suspend fun fetchServiceConfiguration(): AuthorizationServiceConfiguration =
        suspendCoroutine { cont ->
            AuthorizationServiceConfiguration.fetchFromIssuer(
                "http://dex.localtest.me:4190/dex".toUri(),
                object : AuthorizationServiceConfiguration.RetrieveConfigurationCallback {
                    override fun onFetchConfigurationCompleted(
                        serviceConfiguration: AuthorizationServiceConfiguration?,
                        ex: AuthorizationException?
                    ) {
                        if (ex != null || serviceConfiguration == null) {
                            cont.resumeWithException(Exception(ex))
                        } else {
                            cont.resume(serviceConfiguration)
                        }
                    }
                },
                insecureAppAuthConfig.connectionBuilder
            )
        }

    suspend fun onLogin(
        resp: AuthorizationResponse?,
        ex: AuthorizationException?
    ) {
        var authState = authStateStore.state.first() ?: AuthState()
        authState = authState.copyUpdate({ authState -> authState.update(resp, ex) })
        authStateStore.update(authState)

        resp?.let {
            val tokenRequest = it.createTokenExchangeRequest()
            performTokenExchange(authState, tokenRequest)
        }

        authStateStore.update(authState)
    }

    private suspend fun performTokenExchange(
        authState: AuthState,
        tokenRequest: TokenRequest,
    ): Unit =
        suspendCoroutine { cont ->
            service.performTokenRequest(
                tokenRequest,
                clientAuthentication,
                object : AuthorizationService.TokenResponseCallback {
                    override fun onTokenRequestCompleted(
                        response: TokenResponse?,
                        ex: AuthorizationException?
                    ) {
                        authState.update(response, ex)

                        if (response != null) {
                            cont.resume(Unit)
                        } else {
                            cont.resumeWithException(Throwable(ex))
                        }
                    }
                })
        }

    suspend fun <T> withTokens(
        authState: AuthState,
        action: (accessToken: String, idToken: String) -> T
    ): T {
        return withTokens(authState, false, action)
    }

    internal suspend fun <T> withTokens(
        authState: AuthState,
        forceTokenRefresh: Boolean,
        action: (accessToken: String, idToken: String) -> T
    ): T {
        val currentState = state.first()
        if (currentState !is LoginState.LoggedIn) {
            throw IllegalStateException("Not logged in")
        }
        val currentAuthState = currentState.userSession.authState
        if (authState != currentAuthState) {
            throw IllegalStateException("Stale user session")
        }

        // Make a copy to avoid mutating the current state
        val newAuthState = authState.copyUpdate { }

        if (forceTokenRefresh) {
            newAuthState.needsTokenRefresh = true
        }

        try {
            return suspendCoroutine { cont ->
                newAuthState.performActionWithFreshTokens(
                    service,
                    clientAuthentication,
                    object : AuthState.AuthStateAction {
                        override fun execute(
                            accessToken: String?,
                            idToken: String?,
                            ex: AuthorizationException?
                        ) {
                            if (accessToken != null && idToken != null) {
                                val result = action(accessToken, idToken)
                                cont.resume(result)
                            } else {
                                cont.resumeWithException(Exception(ex))
                            }
                        }
                    })
            }
        } finally {
            // The state may have been updated with fresh tokens
            authStateStore.update(newAuthState)
        }
    }

    suspend fun logOut() {
        authStateStore.update(null)
    }
}