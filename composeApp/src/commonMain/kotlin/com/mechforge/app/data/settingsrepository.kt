package com.mechforge.app.data

import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * UI language of the application shell. This is independent of the language chosen
 * for an exported PDF report (that choice lives in the export dialog). The visible
 * names are endonyms, so the picker stays readable whichever language is active.
 */
enum class LanguageMode(val displayName: String) {
    SYSTEM("System"), ARABIC("العربية"), ENGLISH("English");

    companion object {
        fun fromName(name: String?): LanguageMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

class SettingsRepository(private val db: MechForgeDatabase) {

    private val _theme = MutableStateFlow(ThemeMode.fromName(raw(KEY_THEME)))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(LanguageMode.fromName(raw(KEY_LANGUAGE)))
    val language: StateFlow<LanguageMode> = _language.asStateFlow()

    fun raw(key: String): String? =
        db.settingsQueries.getSetting(key).executeAsOneOrNull()?.value_

    fun set(key: String, value: String) {
        db.settingsQueries.upsertSetting(key, value)
        if (key == KEY_THEME) _theme.value = ThemeMode.fromName(value)
        if (key == KEY_LANGUAGE) _language.value = LanguageMode.fromName(value)
    }

    fun setTheme(mode: ThemeMode) = set(KEY_THEME, mode.name)

    fun setLanguage(mode: LanguageMode) = set(KEY_LANGUAGE, mode.name)

    /**
     * Project data printed on an exported calculation sheet (all optional).
     * Stored as plain settings so the last used values are pre-filled next time.
     */
    fun reportValue(key: String): String = raw(key) ?: ""

    fun setReportValue(key: String, value: String) = set(key, value)

    companion object {
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_REPORT_PROJECT = "report_project"
        const val KEY_REPORT_CLIENT = "report_client"
        const val KEY_REPORT_ENGINEER = "report_engineer"
        const val KEY_REPORT_LOCATION = "report_location"
    }
}
