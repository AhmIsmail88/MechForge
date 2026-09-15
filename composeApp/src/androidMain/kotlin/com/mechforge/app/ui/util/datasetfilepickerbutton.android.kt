package com.mechforge.app.ui.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Android actual: system document picker reading the CSV as UTF-8 text. */
@Composable
actual fun DatasetFilePickerButton(label: String, extensions: List<String>, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()?.let(onPicked)
        }
    }
    Button(onClick = { launcher.launch("*/*") }) { Text(label) }
}
