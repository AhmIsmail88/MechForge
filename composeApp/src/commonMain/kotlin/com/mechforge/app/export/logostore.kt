package com.mechforge.app.export

/**
 * Company / engineering-office logo used on the report letterhead (README v2 §36).
 *
 * The image is provided by the user from the device and kept locally by the platform
 * implementation (desktop: `~/.mechforge`, Android: the app's private files directory).
 * The bytes are handed to the PDF renderers, so no platform type leaks into the
 * shared code and no internet or storage permission is required.
 */
expect class LogoStore {

    /** Stores [bytes] as the current logo, replacing any previous one. */
    fun save(bytes: ByteArray): Boolean

    /** Returns the stored logo bytes, or null when no logo has been chosen. */
    fun load(): ByteArray?

    /** Removes the stored logo. */
    fun clear()
}
