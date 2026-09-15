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

/** Desktop actual: native file dialog reading the CSV as text. */
@Composable
actual fun CsvPickerButton(label: String, onPicked: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Choose a CSV dataset"
                fileFilter = FileNameExtensionFilter("Comma separated values (*.csv, *.txt)", "csv", "txt")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val file: File = chooser.selectedFile
                runCatching { file.readText(Charsets.UTF_8) }.getOrNull()?.let(onPicked)
            }
        }
    }) { Text(label) }
}
