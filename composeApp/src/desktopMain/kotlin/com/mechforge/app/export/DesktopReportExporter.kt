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

    /** Writes a paginated A4 PDF report through the native save dialog. */
    override suspend fun savePdf(
        defaultName: String,
        title: String,
        meta: List<Pair<String, String>>,
        blocks: List<ReportBlock>,
    ): String? = withContext(Dispatchers.Swing) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Export PDF report"
            selectedFile = File("$defaultName.pdf")
            fileFilter = FileNameExtensionFilter("PDF report (*.pdf)", "pdf")
        }
        val choice = chooser.showSaveDialog(null)
        if (choice != JFileChooser.APPROVE_OPTION) return@withContext null
        var target = chooser.selectedFile
        if (!target.name.endsWith(".pdf", ignoreCase = true)) {
            target = File(target.parentFile, "${target.name}.pdf")
        }
        if (DesktopPdfReport().write(target, title, meta, blocks)) target.absolutePath else null
    }
}
