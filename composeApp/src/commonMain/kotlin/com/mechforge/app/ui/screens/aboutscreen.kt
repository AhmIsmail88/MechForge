package com.mechforge.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.ui.i18n.LocalStrings

@Composable
fun AboutScreen() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(strings.appName, style = MaterialTheme.typography.headlineLarge)
        Text(
            strings.appTagline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(strings.aboutVersion, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        Text(strings.aboutMissionTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Text(strings.aboutMissionBody,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Text(strings.aboutLocalFirstTitle, style = MaterialTheme.typography.titleMedium)
        Text(strings.aboutLocalFirstBody,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Text(strings.aboutReferencesTitle, style = MaterialTheme.typography.titleMedium)
        Text(strings.aboutReferencesBody,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
