package com.mechforge.app.export

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android report export (README v2 §36).
 *
 * The pilot Android shell has no document picker, so the report is written into the
 * app's own files directory and the resulting path is shown in the UI. The report
 * text can also be copied to the clipboard with the "Copy" action.
 */
class AndroidReportExporter(private val context: Context) : ReportExporter {

    override suspend fun save(defaultName: String, content: String): String? =
        withContext(Dispatchers.IO) {
            val root = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(root, "reports").apply { mkdirs() }
            val file = nextFreeFile(dir, defaultName)
            file.writeText(content, Charsets.UTF_8)
            file.absolutePath
        }

    /** Never overwrites an earlier report: appends _1, _2, … when the name is taken. */
    private fun nextFreeFile(dir: File, defaultName: String, extension: String = "txt"): File {
        val safeName = defaultName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        var candidate = File(dir, "$safeName.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${safeName}_$index.$extension")
            index++
        }
        return candidate
    }

    override suspend fun savePdf(
        defaultName: String,
        title: String,
        meta: List<Pair<String, String>>,
        blocks: List<ReportBlock>,
    ): String? = withContext(Dispatchers.IO) {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(root, "reports").apply { mkdirs() }
        val file = nextFreeFile(dir, defaultName, "pdf")
        if (AndroidPdfReport().write(file, title, meta, blocks)) file.absolutePath else null
    }
}
