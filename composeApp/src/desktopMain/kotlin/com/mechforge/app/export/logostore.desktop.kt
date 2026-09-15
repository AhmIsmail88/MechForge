package com.mechforge.app.export

import java.io.File

/**
 * Desktop logo store: one file under `~/.mechforge`, next to the SQLite database.
 */
actual class LogoStore {

    private val file: File
        get() = File(File(System.getProperty("user.home"), ".mechforge").apply { mkdirs() }, "company_logo.img")

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
