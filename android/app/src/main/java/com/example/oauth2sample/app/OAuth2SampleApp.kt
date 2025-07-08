package com.example.oauth2sample.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.oauth2sample.app.auth.AuthStatusScreen
import com.example.oauth2sample.app.auth.LoginFrame
import com.example.oauth2sample.app.auth.UserSessionService
import com.example.oauth2sample.ui.MainScreen
import com.example.oauth2sample.ui.ScreenThatRequiresLogin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable

sealed class Destinations {
    @Serializable
    data object Main

    @Serializable
    data object ScreenThatRequiresLogin

    @Serializable
    data object AuthStatus

}

/**
 * Nav host and app-wide component logic for the app.
 */
@Composable
fun OAuth2SampleApp() {
    val navController = rememberNavController()

    val applicationContext = LocalContext.current.applicationContext

    val applicationCoroutineScope = CoroutineScope(SupervisorJob())

    val userSessionService = remember {
        UserSessionService(applicationContext, applicationCoroutineScope)
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.Main,
    ) {
        composable<Destinations.Main> {
            MainScreen(
                onNavigateToAuthStatus = {
                    navController.navigate(Destinations.AuthStatus)
                },
                onNavigateToRequiresLogin = {
                    navController.navigate(Destinations.ScreenThatRequiresLogin)
                },
            )
        }
        composable<Destinations.AuthStatus> {
            AuthStatusScreen(userSessionService)
        }
        composable<Destinations.ScreenThatRequiresLogin> {
            LoginFrame(userSessionService) { userSession ->
                ScreenThatRequiresLogin(userSession)
            }
        }
    }
}