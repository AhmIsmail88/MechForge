package com.mechforge.app.ui.i18n

import java.util.Locale

/** Android actual: the JVM default locale follows the system language setting. */
actual fun systemLanguageIsArabic(): Boolean =
    Locale.getDefault().language == "ar"
