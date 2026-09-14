package com.mechforge.app.ui.i18n

import java.util.Locale

/** Desktop actual: the JVM default locale follows the OS language setting. */
actual fun systemLanguageIsArabic(): Boolean =
    Locale.getDefault().language == "ar"
