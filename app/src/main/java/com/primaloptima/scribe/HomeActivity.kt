package com.primaloptima.scribe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.ui.screens.HomeScreen
import com.primaloptima.scribe.ui.theme.ScribeComposeTheme
import com.primaloptima.scribe.util.PrefsManager
import com.primaloptima.scribe.viewmodel.HomeViewModel

class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        // ── Set the splash theme BEFORE installSplashScreen() ─────────────────
        // setTheme() here overrides the theme declared in AndroidManifest.xml
        // for this activity. installSplashScreen() reads windowSplashScreenBackground
        // and windowSplashScreenAnimatedIcon from whichever theme is active at
        // the moment it is called, so this must come first.
        val prefs = PrefsManager(this)
        setTheme(splashStyleFor(prefs.activeThemeId))

        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Hold the splash until DB emits its first result
        splashScreen.setKeepOnScreenCondition { vm.books.value == null }

        // Fade out instead of snapping
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view
                .animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { provider.remove() }
                .start()
        }

        setContent {
            ScribeComposeTheme {
                HomeScreen(
                    vm = vm,
                    onOpenBook     = { book -> openBook(book) },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onOpenSheets   = { startActivity(Intent(this, SheetsActivity::class.java)) },
                    onOpenThemes   = { startActivity(Intent(this, ThemeListActivity::class.java)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshStreak()
    }

    private fun openBook(book: Book) {
        startActivity(
            Intent(this, BookActivity::class.java)
                .putExtra(BookActivity.EXTRA_BOOK_ID, book.id)
                .putExtra(BookActivity.EXTRA_BOOK_TITLE, book.title)
        )
    }

    /**
     * Maps a saved theme ID to the matching pre-defined splash style.
     * Each style lives in res/values/scribe_splash_dynamic_theme.xml (API < 31)
     * and res/values-v31/scribe_splash_dynamic_theme.xml (API 31+ adds icon color).
     */
    private fun splashStyleFor(themeId: String): Int = when (themeId) {
        "obsidian"   -> R.style.Theme_Scribe_Splash_Obsidian
        "midnight"   -> R.style.Theme_Scribe_Splash_Midnight
        "focus"      -> R.style.Theme_Scribe_Splash_Focus
        "paper"      -> R.style.Theme_Scribe_Splash_Paper
        "sepia"      -> R.style.Theme_Scribe_Splash_Sepia
        "typewriter" -> R.style.Theme_Scribe_Splash_Typewriter
        else         -> R.style.Theme_Scribe_Splash_Custom
    }
}
