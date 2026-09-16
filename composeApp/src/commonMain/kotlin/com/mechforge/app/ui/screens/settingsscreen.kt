package com.mechforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.data.LanguageMode
import com.mechforge.app.data.ReportLanguage
import com.mechforge.app.data.SettingsRepository
import com.mechforge.app.data.ThemeMode
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.ui.util.LogoPickerButton

/**
 * Settings. All chrome text comes from the UI strings layer (Part C), so switching
 * the app language to Arabic switches these labels too and mirrors the layout.
 *
 * Each option is a full-width clickable row (not just the radio circle): a 48dp+
 * touch target and one obvious hit area, which is also what makes the control
 * usable one-handed on a phone.
 */
@Composable
fun SettingsScreen(deps: AppDependencies) {
    val theme by deps.settings.theme.collectAsState()
    val language by deps.settings.language.collectAsState()
    val reportLanguage by deps.settings.reportLanguage.collectAsState()
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Text(strings.settingsAppearance, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        for (mode in ThemeMode.entries) {
            OptionRow(
                selected = theme == mode,
                label = when (mode) {
                    ThemeMode.SYSTEM -> strings.themeSystem
                    ThemeMode.LIGHT -> strings.themeLight
                    ThemeMode.DARK -> strings.themeDark
                },
                onClick = { deps.settings.setTheme(mode) },
            )
        }

        Spacer(Modifier.height(16.dp))

        // App language. Independent of the language chosen for an exported PDF
        // report; ARABIC mirrors the whole UI to RTL (see MechForgeTheme).
        Text(strings.settingsLanguage, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        for (mode in LanguageMode.entries) {
            OptionRow(
                selected = language == mode,
                // Arabic and English stay as endonyms (readable in any language); "System"
                // is translated because it is not an endonym.
                label = when (mode) {
                    LanguageMode.SYSTEM -> strings.languageSystem
                    LanguageMode.ARABIC -> strings.languageArabic
                    LanguageMode.ENGLISH -> strings.languageEnglish
                },
                onClick = { deps.settings.setLanguage(mode) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            strings.settingsLanguagePhaseNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Report language: the exported calculation sheet has its own language, so an
        // English interface can still issue an Arabic report (and the other way round).
        Text(strings.settingsReportLanguage, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        OptionRow(
            selected = reportLanguage == ReportLanguage.FOLLOW_APP,
            label = strings.reportLanguageFollowApp,
            onClick = { deps.settings.setReportLanguage(ReportLanguage.FOLLOW_APP) },
        )
        OptionRow(
            selected = reportLanguage == ReportLanguage.ARABIC,
            label = strings.reportLanguageArabic,
            onClick = { deps.settings.setReportLanguage(ReportLanguage.ARABIC) },
        )
        OptionRow(
            selected = reportLanguage == ReportLanguage.ENGLISH,
            label = strings.reportLanguageEnglish,
            onClick = { deps.settings.setReportLanguage(ReportLanguage.ENGLISH) },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.reportLanguageNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Project data printed on every exported calculation sheet (letterhead).
        Text(strings.settingsReportDetails, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Text(
            strings.settingsReportDetailsNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        var projectName by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_PROJECT)) }
        var clientName by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_CLIENT)) }
        var engineerName by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_ENGINEER)) }
        var locationName by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_LOCATION)) }
        ReportField(strings.settingsReportProject, projectName) {
            projectName = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_PROJECT, it)
        }
        ReportField(strings.settingsReportClient, clientName) {
            clientName = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_CLIENT, it)
        }
        ReportField(strings.settingsReportEngineer, engineerName) {
            engineerName = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_ENGINEER, it)
        }
        ReportField(strings.settingsReportLocation, locationName) {
            locationName = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_LOCATION, it)
        }
        var reportNo by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_NO)) }
        var reportRev by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_REV)) }
        var checkedBy by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_CHECKED)) }
        ReportField(strings.settingsReportNo, reportNo) {
            reportNo = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_NO, it)
        }
        ReportField(strings.settingsReportRevision, reportRev) {
            reportRev = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_REV, it)
        }
        var reportCode by remember { mutableStateOf(deps.settings.reportValue(SettingsRepository.KEY_REPORT_CODE)) }
        ReportField(strings.settingsReportCode, reportCode) {
            reportCode = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_CODE, it)
        }
        ReportField(strings.settingsReportCheckedBy, checkedBy) {
            checkedBy = it
            deps.settings.setReportValue(SettingsRepository.KEY_REPORT_CHECKED, it)
        }

        Spacer(Modifier.height(16.dp))

        // Company / office logo used on the PDF letterhead.
        Text(strings.settingsLogo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        var logoBytes by remember { mutableStateOf(deps.logoStore.load()?.size ?: 0) }
        Text(
            if (logoBytes > 0) strings.settingsLogoSelected else strings.settingsLogoNone,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoPickerButton(strings.settingsChooseLogo) { bytes ->
                if (deps.logoStore.save(bytes)) logoBytes = bytes.size
            }
            if (logoBytes > 0) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    deps.logoStore.clear()
                    logoBytes = 0
                }) { Text(strings.remove) }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text(
            strings.settingsPrivacyNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One text field of the report letterhead; writes through to settings on every keystroke. */
@Composable
private fun ReportField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/** One selectable option: whole row tappable, radio is decorative. */
@Composable
private fun OptionRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
