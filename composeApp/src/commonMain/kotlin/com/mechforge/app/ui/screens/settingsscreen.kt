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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.data.LanguageMode
import com.mechforge.app.data.ThemeMode
import com.mechforge.app.ui.i18n.LocalStrings

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
                label = mode.displayName,
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
                label = mode.displayName,
                onClick = { deps.settings.setLanguage(mode) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            strings.settingsLanguagePhaseNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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
