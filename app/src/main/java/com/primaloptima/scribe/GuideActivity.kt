package com.primaloptima.scribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.primaloptima.scribe.ui.screens.GuideScreen
import com.primaloptima.scribe.ui.theme.ScribeComposeTheme

class GuideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScribeComposeTheme {
                GuideScreen(onBack = { finish() })
            }
        }
    }
}
