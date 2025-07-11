package com.example.oauth2sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.oauth2sample.app.auth.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Composable
fun ScreenThatRequiresLogin(userSession: UserSession) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .padding(8.dp)
        ) {
            Text("Hello, ${userSession.username}")

            var successBody by remember { mutableStateOf<String?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    coroutineScope.launch {
                        successBody = null
                        errorMessage = null

                        try {
                            successBody = sendApiRequest(null)
                        } catch (ex: Exception) {
                            errorMessage = ex.toString()
                        }
                    }
                }
            ) {
                Text("Send API request without Authorization header")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        successBody = ""
                        errorMessage = ""

                        try {
                            userSession.withTokens { accessToken, idToken ->
                                successBody = sendApiRequest("Bearer ${accessToken}")
                            }
                        } catch (ex: Exception) {
                            errorMessage = ex.toString()
                        }
                    }
                }
            ) {
                Text("Send API request with Authorization header")
            }

            val color = if (successBody != null) {
                Color.Green
            } else {
                Color.Red
            }
            Text(
                text = successBody ?: errorMessage ?: "",
                color = color,
            )
        }
    }
}

suspend fun sendApiRequest(authorization: String?): String {
    return withContext(Dispatchers.IO) {
        val url = URL("http://api.oauth2-proxy.localtest.me/status")
        val connection = url.openConnection() as HttpURLConnection

        authorization?.let {
            connection.setRequestProperty("Authorization", authorization)
        }

        connection.connect()

        val code = connection.getResponseCode()
        if (code / 100 != 2) {
            throw Exception("Got response code $code")
        }

        suspendCoroutine { cont ->
            try {
                val body = InputStreamReader(connection.getInputStream()).use { reader ->
                    reader.readText()
                }
                cont.resume(body)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }
}
