package com.mechforge.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.mechforge.app.ui.MechForgeApp
import com.mechforge.app.ui.i18n.resolveStrings
import com.mechforge.app.ui.theme.MechForgeTheme
import java.util.Locale

/**
 * Android entry point: a thin shell around the *same* shared Compose UI and the
 * *same* `core` engine used by the desktop app (README v2 §6, §41 Phase 1).
 * No calculation or UI logic lives here.
 *
 * The chosen language is also pushed into the platform configuration. Mirroring the
 * layout alone (`LocalLayoutDirection`, done in MechForgeTheme) is not enough:
 * without an Arabic platform locale the text engine keeps an LTR bidi base, which
 * breaks Arabic words in the middle and moves the trailing period of an English
 * sentence to the start of the next line. The layout direction itself is left to
 * `LocalLayoutDirection`; only the locale is set here.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deps = (application as MechForgeApplication).dependencies
        setContent {
            val themeMode by deps.settings.theme.collectAsState()
            val languageMode by deps.settings.language.collectAsState()
            val strings = resolveStrings(languageMode)

            val baseConfiguration = LocalConfiguration.current
            val localeConfiguration = remember(strings.isRtl) {
                Configuration(baseConfiguration).apply {
                    setLocale(if (strings.isRtl) Locale("ar") else Locale("en"))
                }
            }

            CompositionLocalProvider(LocalConfiguration provides localeConfiguration) {
                MechForgeTheme(themeMode, languageMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                    ) {
                        MechForgeApp(deps)
                    }
                }
            }
        }
    }
}
