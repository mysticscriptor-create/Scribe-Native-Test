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
import com.primaloptima.scribe.viewmodel.HomeViewModel

class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate()
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Hold the system splash until the DB has emitted its first result.
        // vm.books.value is null until Room's first emission — then it becomes
        // a List<Book> (possibly empty), which is our signal that we're ready.
        splashScreen.setKeepOnScreenCondition {
            vm.books.value == null
        }

        // Fade the splash out instead of snapping to the home screen.
        splashScreen.setOnExitAnimationListener { splashViewProvider ->
            splashViewProvider.view
                .animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { splashViewProvider.remove() }
                .start()
        }

        setContent {
            ScribeComposeTheme {
                HomeScreen(
                    vm = vm,
                    onOpenBook = { book -> openBook(book) },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onOpenSheets = { startActivity(Intent(this, SheetsActivity::class.java)) },
                    onOpenThemes = { startActivity(Intent(this, ThemeListActivity::class.java)) }
                )
            }
        }
    }

    private fun openBook(book: Book) {
        startActivity(
            Intent(this, BookActivity::class.java)
                .putExtra(BookActivity.EXTRA_BOOK_ID, book.id)
                .putExtra(BookActivity.EXTRA_BOOK_TITLE, book.title)
        )
    }
}
