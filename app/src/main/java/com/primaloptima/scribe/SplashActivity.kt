package com.primaloptima.scribe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.primaloptima.scribe.ui.splash.ScribeSplash
import com.primaloptima.scribe.util.ThemeManager

/**
 * Theme-aware launch activity.
 *
 * The Android 12 system splash hands off to the Compose splash below. The
 * Compose splash reads the user's persisted Scribe theme, so the background
 * and mark remain correct even when the user last selected a custom theme.
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set icon appearance to match the user's theme even before
        // ScribeComposeTheme's SideEffect runs (first frame).
        val activeTheme = ThemeManager(this).activeTheme()
        val backgroundColor = ThemeManager.parseColor(activeTheme.colors.background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            val darkBackground = ThemeManager.isColorDark(backgroundColor)
            isAppearanceLightStatusBars = !darkBackground
            isAppearanceLightNavigationBars = !darkBackground
        }

        setContent {
            ScribeSplash(
                onFinished = {
                    startActivity(
                        Intent(this, HomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    )
                    finish()
                }
            )
        }
    }
}