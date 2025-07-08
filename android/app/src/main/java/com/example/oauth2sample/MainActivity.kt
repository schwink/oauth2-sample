package com.example.oauth2sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.oauth2sample.app.OAuth2SampleApp
import com.example.oauth2sample.ui.theme.OAuth2SampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OAuth2SampleTheme {
                OAuth2SampleApp()
            }
        }
    }
}
