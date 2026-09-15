package com.mechforge.app.ui.util

import androidx.compose.runtime.Composable

/**
 * Platform image picker for the company logo shown on PDF reports.
 *
 * Android opens the system photo picker (no storage permission); desktop opens a
 * native file dialog running on the Swing event thread. Implementations report the
 * raw image bytes, which the shared code hands to [com.mechforge.app.export.LogoStore].
 */
@Composable
expect fun LogoPickerButton(label: String, onPicked: (ByteArray) -> Unit)
