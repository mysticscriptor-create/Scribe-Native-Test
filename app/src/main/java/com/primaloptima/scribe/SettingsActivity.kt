package com.primaloptima.scribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.primaloptima.scribe.ui.screens.SettingsScreen
import com.primaloptima.scribe.ui.theme.ScribeComposeTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScribeComposeTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
