package com.example.oauth2sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oauth2sample.app.auth.UserSession

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
        }
    }
}