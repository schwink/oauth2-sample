package com.example.oauth2sample.app.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ResponseTypeValues

/**
 * A container that ensures the user is logged in (showing the login screen if not), then displays
 * the specified content, providing a [UserSession].
 */
@Composable
fun LoginFrame(
    userSessionService: UserSessionService,
    content: @Composable (userSession: UserSession) -> Unit,
) {
    val state by userSessionService.state.collectAsState()
    when (state) {
        UserSessionService.LoginState.Loading -> {
            LoadingScreen()
        }

        is UserSessionService.LoginState.LoggedOut -> {
            LoginScreen(userSessionService = userSessionService)
        }

        is UserSessionService.LoginState.LoggedIn -> {
            content((state as UserSessionService.LoginState.LoggedIn).userSession)
        }
    }

}

@Composable
private fun LoadingScreen() {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp)
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun LoginScreen(userSessionService: UserSessionService) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp)
        ) {
            val scope = rememberCoroutineScope()

            val loginResult = rememberLauncherForActivityResult(
                contract = StartActivityForResult(),
                onResult = { result: ActivityResult ->
                    val data = result.data
                    if (data != null) {
                        val resp: AuthorizationResponse? =
                            result.data?.let { AuthorizationResponse.fromIntent(it) }
                        val ex: AuthorizationException? =
                            result.data?.let { AuthorizationException.fromIntent(it) }

                        scope.launch {
                            userSessionService.onLogin(resp, ex)
                        }
                    }
                }
            )

            Button(onClick = {
                scope.launch {
                    val serviceConfiguration = userSessionService.serviceConfiguration()

                    val authRequest = AuthorizationRequest.Builder(
                        serviceConfiguration,
                        "oauth2-proxy",  // the client ID, typically pre-registered and static
                        ResponseTypeValues.CODE,
                        "com.example.oauth2sample.auth://oauth2/callback".toUri()
                    )
                        // offline_access scope is required to get a refresh token from the server
                        .setScope("openid email profile offline_access")
                        .setLoginHint("admin@example.com")
                        .build()

                    val intent =
                        userSessionService.service.getAuthorizationRequestIntent(authRequest)

                    loginResult.launch(intent)
                }
            }) {
                Text("Log In")
            }

            Text("User name is admin@example.com")
            Text("Password is \"password\"")
        }
    }
}
