package app.umaia.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("umaia_app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.LIGHT)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val supportedLanguages = setOf("en", "ru", "kk")
    private val _language = MutableStateFlow(
        prefs.getString("language", null) ?: defaultLanguage()
    )
    val language: StateFlow<String> = _language.asStateFlow()

    private fun defaultLanguage(): String {
        val systemLang = runCatching {
            java.util.Locale.getDefault().language
        }.getOrNull()
        return if (systemLang in supportedLanguages) systemLang!! else "ru"
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    /** Call once on app startup to restore persisted locale. */
    fun applyLocale() {
        val lang = _language.value
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }
}
