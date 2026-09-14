package com.mechforge.app.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** Desktop report export: the pilot's native save dialog, unchanged behaviour. */
class DesktopReportExporter : ReportExporter {

    override suspend fun save(defaultName: String, content: String): String? =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Export calculation report"
                selectedFile = File("$defaultName.txt")
                fileFilter = FileNameExtensionFilter("Text report (*.txt)", "txt")
            }
            val choice = chooser.showSaveDialog(null)
            if (choice != JFileChooser.APPROVE_OPTION) return@withContext null
            var target = chooser.selectedFile
            if (!target.name.endsWith(".txt", ignoreCase = true)) {
                target = File(target.parentFile, "${target.name}.txt")
            }
            target.writeText(content, Charsets.UTF_8)
            target.absolutePath
        }
}
