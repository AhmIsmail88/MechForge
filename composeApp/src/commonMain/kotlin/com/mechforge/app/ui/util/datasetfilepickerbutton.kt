package com.mechforge.app.ui.util

import androidx.compose.runtime.Composable

/**
 * Platform file picker for the reference library importer (CSV or JSON).
 * Android opens the system document picker; desktop opens a native file dialog with the
 * given extensions. Implementations return the file TEXT (UTF-8).
 */
@Composable
expect fun DatasetFilePickerButton(label: String, extensions: List<String>, onPicked: (String) -> Unit)
