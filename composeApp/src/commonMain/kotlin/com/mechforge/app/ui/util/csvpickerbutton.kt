package com.mechforge.app.ui.util

import androidx.compose.runtime.Composable

/**
 * Platform CSV picker for the reference library importer.
 * Android opens the system document picker; desktop opens a native file dialog.
 * Implementations return the file TEXT (UTF-8).
 */
@Composable
expect fun CsvPickerButton(label: String, onPicked: (String) -> Unit)
