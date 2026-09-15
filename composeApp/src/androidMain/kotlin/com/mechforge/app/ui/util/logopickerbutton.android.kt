package com.mechforge.app.ui.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Android actual: the system photo picker — no storage permission needed. */
@Composable
actual fun LogoPickerButton(label: String, onPicked: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()?.let(onPicked)
        }
    }
    Button(onClick = { launcher.launch("image/*") }) { Text(label) }
}
