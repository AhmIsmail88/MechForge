package com.mechforge.app.export

import android.content.Context
import java.io.File

/**
 * Android logo store: one file in the app's private files directory. Nothing is
 * exposed to the gallery or to other apps.
 */
actual class LogoStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "company_logo.img")

    actual fun save(bytes: ByteArray): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    }.getOrDefault(false)

    actual fun load(): ByteArray? =
        if (file.exists()) runCatching { file.readBytes() }.getOrNull() else null

    actual fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }
}
