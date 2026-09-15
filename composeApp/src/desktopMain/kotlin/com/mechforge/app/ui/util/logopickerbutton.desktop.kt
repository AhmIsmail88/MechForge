package com.mechforge.app.ui.util

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** Desktop actual: a native file dialog on the Swing event thread. */
@Composable
actual fun LogoPickerButton(label: String, onPicked: (ByteArray) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Choose company logo"
                fileFilter = FileNameExtensionFilter("Images (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val file: File = chooser.selectedFile
                runCatching { file.readBytes() }.getOrNull()?.let(onPicked)
            }
        }
    }) { Text(label) }
}
