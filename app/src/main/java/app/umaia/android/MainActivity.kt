package app.umaia.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import app.umaia.android.data.local.AppPreferences
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.data.local.ThemeMode
import app.umaia.android.ui.navigation.NavGraph
import app.umaia.android.ui.strings.EnStrings
import app.umaia.android.ui.strings.KkStrings
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.strings.RuStrings
import app.umaia.android.ui.theme.UmaiaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var gamePreferences: GamePreferences
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences.applyLocale()
        enableEdgeToEdge()
        setContent {
            val themeMode by appPreferences.themeMode.collectAsState()
            val language by appPreferences.language.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> systemDark
            }
            val strings = when (language) {
                "ru" -> RuStrings
                "kk" -> KkStrings
                else -> EnStrings
            }
            androidx.compose.runtime.CompositionLocalProvider(LocalStrings provides strings) {
                UmaiaTheme(darkTheme = isDark) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        gamePrefs = gamePreferences
                    )
                }
            }
        }
    }
}
