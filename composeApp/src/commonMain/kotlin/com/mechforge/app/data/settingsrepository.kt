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
 * UI language of the application shell (its own setting, and its own RTL decision).
 * The visible names are endonyms, so the picker stays readable whichever language
 * is active.
 */
enum class LanguageMode(val displayName: String) {
    SYSTEM("System"), ARABIC("العربية"), ENGLISH("English");

    companion object {
        fun fromName(name: String?): LanguageMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * Language of an exported report. Fully independent of the UI language, so an English
 * interface can still issue an Arabic calculation sheet (and the other way round).
 */
enum class ReportLanguage {
    FOLLOW_APP, ARABIC, ENGLISH;

    companion object {
        fun fromName(name: String?): ReportLanguage =
            entries.firstOrNull { it.name == name } ?: FOLLOW_APP
    }
}

class SettingsRepository(private val db: MechForgeDatabase) {

    private val _theme = MutableStateFlow(ThemeMode.fromName(raw(KEY_THEME)))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(LanguageMode.fromName(raw(KEY_LANGUAGE)))
    val language: StateFlow<LanguageMode> = _language.asStateFlow()

    private val _reportLanguage = MutableStateFlow(ReportLanguage.fromName(raw(KEY_REPORT_LANGUAGE)))
    val reportLanguage: StateFlow<ReportLanguage> = _reportLanguage.asStateFlow()

    fun raw(key: String): String? =
        db.settingsQueries.getSetting(key).executeAsOneOrNull()?.value_

    fun set(key: String, value: String) {
        db.settingsQueries.upsertSetting(key, value)
        if (key == KEY_THEME) _theme.value = ThemeMode.fromName(value)
        if (key == KEY_LANGUAGE) _language.value = LanguageMode.fromName(value)
        if (key == KEY_REPORT_LANGUAGE) _reportLanguage.value = ReportLanguage.fromName(value)
    }

    fun setTheme(mode: ThemeMode) = set(KEY_THEME, mode.name)

    fun setLanguage(mode: LanguageMode) = set(KEY_LANGUAGE, mode.name)

    fun setReportLanguage(mode: ReportLanguage) = set(KEY_REPORT_LANGUAGE, mode.name)

    /** True when an exported report should be written right-to-left (Arabic). */
    fun reportIsArabic(): Boolean = when (_reportLanguage.value) {
        ReportLanguage.ARABIC -> true
        ReportLanguage.ENGLISH -> false
        ReportLanguage.FOLLOW_APP -> _language.value == LanguageMode.ARABIC
    }

    /**
     * Project data printed on an exported calculation sheet (all optional).
     * Stored as plain settings so the last used values are pre-filled next time.
     */
    fun reportValue(key: String): String = raw(key) ?: ""

    fun setReportValue(key: String, value: String) = set(key, value)

    companion object {
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_REPORT_LANGUAGE = "report_language"
        const val KEY_REPORT_PROJECT = "report_project"
        const val KEY_REPORT_CLIENT = "report_client"
        const val KEY_REPORT_ENGINEER = "report_engineer"
        const val KEY_REPORT_LOCATION = "report_location"
        const val KEY_REPORT_NO = "report_no"
        const val KEY_REPORT_REV = "report_rev"
        const val KEY_REPORT_CODE = "report_code"
        const val KEY_REPORT_CHECKED = "report_checked"
    }
}
