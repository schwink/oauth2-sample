package com.example.oauth2sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onNavigateToAuthStatus: () -> Unit,
    onNavigateToRequiresLogin: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            Button(onClick = onNavigateToAuthStatus) {
                Text("View current auth status")
            }
            Button(onClick = onNavigateToRequiresLogin) {
                Text("Visit a screen that requires login")
            }
        }
    }
}