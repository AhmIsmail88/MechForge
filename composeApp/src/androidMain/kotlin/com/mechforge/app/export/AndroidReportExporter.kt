package com.mechforge.app.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android report export.
 *
 * Text reports keep the old behaviour (app-private files directory). PDF reports are
 * written into the public **Downloads/MechForge** collection through MediaStore, so
 * the file is visible in any file manager / Google Files without any storage
 * permission, and a share sheet is opened right after the export so the report can
 * be opened, sent or saved to Drive/WhatsApp immediately.
 */
class AndroidReportExporter(private val context: Context) : ReportExporter {

    override suspend fun save(defaultName: String, content: String): String? =
        withContext(Dispatchers.IO) {
            val root = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(root, "reports").apply { mkdirs() }
            val file = nextFreeFile(dir, defaultName, "txt")
            file.writeText(content, Charsets.UTF_8)
            file.absolutePath
        }

    /** Writes a paginated A4 PDF into Downloads/MechForge and offers the share sheet. */
    override suspend fun savePdf(
        defaultName: String,
        title: String,
        meta: List<Pair<String, String>>,
        blocks: List<ReportBlock>,
        logo: ByteArray?,
        rtl: Boolean,
    ): String? = withContext(Dispatchers.IO) {
        val safeName = defaultName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val fileName = safeName + "_" + System.currentTimeMillis() + ".pdf"

        val cache = File(context.cacheDir, "reports").apply { mkdirs() }
        val tmp = File(cache, fileName)
        val rendered = AndroidPdfReport(logo, rtl).write(tmp, title, meta, blocks)
        if (!rendered || !tmp.exists()) {
            tmp.delete()
            return@withContext null
        }

        val uri: Uri? = try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/MechForge",
                )
            }
            val target = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values,
            )
            if (target != null) {
                context.contentResolver.openOutputStream(target)?.use { out ->
                    tmp.inputStream().use { it.copyTo(out) }
                }
            }
            target
        } catch (t: Throwable) {
            null
        }

        if (uri == null) {
            // MediaStore unavailable: keep the file in app storage instead of failing.
            val fallbackDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports")
                .apply { mkdirs() }
            val fallback = File(fallbackDir, fileName)
            tmp.copyTo(fallback, overwrite = true)
            tmp.delete()
            return@withContext fallback.absolutePath
        }

        tmp.delete()
        offerShare(uri)
        notifyReady(uri, fileName)
        "Downloads/MechForge/$fileName"
    }

    /** Opens the system share sheet so the report can be opened or sent straight away. */
    private fun offerShare(uri: Uri) {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "MechForge report")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Throwable) {
            // Sharing is a convenience; never fail the export because of it.
        }
    }

    /** Never overwrites an earlier report: appends _1, _2, ... when the name is taken. */
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
    /** Posts a notification with Open and Share actions so the report stays reachable. */
    private fun notifyReady(uri: Uri, fileName: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MechForge reports",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Calculation reports exported as PDF" }
                nm.createNotificationChannel(channel)
            }
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val openPending = PendingIntent.getActivity(
                context, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val sharePending = PendingIntent.getActivity(
                context, 2, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("MechForge report ready")
                .setContentText(fileName)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .addAction(0, "Share", sharePending)
                .build()
            nm.notify(1001, notification)
        } catch (t: Throwable) {
            // a notification is a convenience; never fail the export because of it
        }
    }

    private companion object {
        const val CHANNEL_ID = "mechforge_reports"
    }
}
