package com.example.oauth2sample.app.auth

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oauth2sample.ui.theme.Typography
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun AuthStatusScreen(userSessionService: UserSessionService) {
    val authStateStore by remember { derivedStateOf { userSessionService.authStateStore } }
    val userSessionState by userSessionService.state.collectAsState()

    val authState by authStateStore.authState.collectAsState(null)
    val configuration by authStateStore.serviceConfiguration.collectAsState(null)

    val sessionInfo: List<Pair<String, String?>> by remember {
        derivedStateOf {
            listOf(
                Pair("UserSessionState", userSessionState.javaClass.simpleName),
            )
        }
    }

    val configurationInfo: List<Pair<String, String?>> by remember {
        derivedStateOf {
            listOf<Pair<String, String?>>(
                Pair("authorizationEndpoint", configuration?.authorizationEndpoint?.toString()),
                Pair("tokenEndpoint", configuration?.tokenEndpoint?.toString()),
                Pair("endSessionEndpoint", configuration?.endSessionEndpoint?.toString()),
                Pair("registrationEndpoint", configuration?.registrationEndpoint?.toString()),
            )
        }
    }

    val tokenInfo: List<Pair<String, String?>> by remember {
        derivedStateOf {
            val idTokenCustomClaimsJson = JSONObject()
            authState?.parsedIdToken?.additionalClaims?.forEach { (key, value) ->
                idTokenCustomClaimsJson.put(key, value)
            }
            val idTokenCustomClaims = idTokenCustomClaimsJson.toString(2)

            listOf<Pair<String, String?>>(
                Pair("isAuthorized", authState?.isAuthorized?.toString()),
                Pair("scope", authState?.scope),
                Pair("refreshToken", authState?.refreshToken),
                Pair("needsTokenRefresh", authState?.needsTokenRefresh?.toString()),
                Pair("accessToken", authState?.accessToken),
                Pair("idToken", authState?.idToken),
                Pair("idToken custom claims", idTokenCustomClaims),
            )
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            val scope = rememberCoroutineScope()

            var errorMessage by remember { mutableStateOf("") }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        userSessionService.logOut()
                    }
                }
            ) {
                Text("Clear client auth store")
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val userSession =
                            (userSessionState as UserSessionService.LoginState.LoggedIn).userSession
                        try {
                            userSession.withTokens { accessToken, idToken ->
                                Log.w(
                                    "UserSession",
                                    "withTokens accessToken=$accessToken idToken=$idToken"
                                )
                            }
                        } catch (e: Throwable) {
                            errorMessage = e.toString()
                        }
                    }
                },
                enabled = userSessionState is UserSessionService.LoginState.LoggedIn
            ) {
                Text("Get valid tokens")
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val userSession =
                            (userSessionState as UserSessionService.LoginState.LoggedIn).userSession
                        try {
                            userSessionService.withTokens(
                                authState = userSession.authState,
                                forceTokenRefresh = true,
                            ) { accessToken, idToken ->
                                Log.w(
                                    "UserSession",
                                    "withTokens accessToken=$accessToken idToken=$idToken"
                                )
                            }
                        } catch (e: Throwable) {
                            errorMessage = e.toString()
                        }
                    }
                },
                enabled = userSessionState is UserSessionService.LoginState.LoggedIn
            ) {
                Text("Force refresh tokens")
            }

            Text(
                text = errorMessage,
            )

            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                item {
                    Text(
                        text = "Session",
                        style = Typography.titleMedium,
                    )
                }
                items(sessionInfo) { (label, value) ->
                    TableRow(label, value)
                }

                item {
                    Text(
                        text = "Configuration",
                        style = Typography.titleMedium,
                    )
                }
                items(configurationInfo) { (label, value) ->
                    TableRow(label, value)
                }

                item {
                    Text(
                        text = "Tokens",
                        style = Typography.titleMedium,
                    )
                }
                items(tokenInfo) { (label, value) ->
                    TableRow(label, value)
                }
            }
        }
    }
}

@Composable
private fun TableRow(label: String, value: String?) {
    Row(modifier = Modifier.padding(8.dp)) {
        Text(
            modifier = Modifier.weight(1f),
            style = Typography.labelMedium,
            text = label
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value ?: "<null>"
        )
    }
}
