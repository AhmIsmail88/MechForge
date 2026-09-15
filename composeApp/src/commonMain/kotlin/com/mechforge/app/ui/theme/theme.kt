package com.mechforge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mechforge.app.data.LanguageMode
import com.mechforge.app.data.ThemeMode
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.ui.i18n.resolveStrings

// MechForge visual identity (README v2 §29/§31): deep engineering blue, graphite,
// electric cyan accent. Professional, clean, high readability — light and dark.
//
// The four brand colours below are the app's colour identity and are unchanged by
// the design pass; everything else about the theme (typography, shapes, semantic
// state colours, spacing) is layered on top in the files next to this one.
internal val EngineeringBlue = Color(0xFF1B4F8A)
internal val EngineeringBlueContainer = Color(0xFFD5E4F7)
internal val Graphite = Color(0xFF3C4043)
internal val Cyan = Color(0xFF00A5C4)

private val LightColors = lightColorScheme(
    primary = EngineeringBlue,
    onPrimary = Color.White,
    primaryContainer = EngineeringBlueContainer,
    onPrimaryContainer = Color(0xFF0B2C4F),
    secondary = Graphite,
    onSecondary = Color.White,
    tertiary = Cyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F2F8),
    // Glass pass: the shell paints the backdrop, so every surface is translucent and
    // Material components (cards, fields, dialogs, menus) become glass for free.
    background = Color(0xFFEDF2FB),
    onBackground = Color(0xFF141A24),
    surface = Color(0x9EFFFFFF),
    onSurface = Color(0xFF141A24),
    surfaceVariant = Color(0x7AFFFFFF),
    onSurfaceVariant = Color(0xFF3A4453),
    surfaceContainerLowest = Color(0x70FFFFFF),
    surfaceContainerLow = Color(0xB8FFFFFF),
    surfaceContainer = Color(0xC6FFFFFF),
    surfaceContainerHigh = Color(0xD8FFFFFF),
    surfaceContainerHighest = Color(0xE6FFFFFF),
    outline = Color(0x33101828),
    outlineVariant = Color(0x1F101828),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC5F5),
    onPrimary = Color(0xFF10283F),
    primaryContainer = Color(0xFF2A5480),
    onPrimaryContainer = Color(0xFFD5E4F7),
    secondary = Color(0xFFBAC0C6),
    onSecondary = Color(0xFF25282B),
    tertiary = Color(0xFF4DD0E1),
    onTertiary = Color(0xFF062F35),
    // Glass pass: translucent panes over the deep engineering backdrop.
    background = Color(0xFF070B16),
    onBackground = Color(0xFFE6ECF8),
    surface = Color(0x1AFFFFFF),
    onSurface = Color(0xFFE6ECF8),
    surfaceVariant = Color(0x24FFFFFF),
    onSurfaceVariant = Color(0xFFC3CCDA),
    surfaceContainerLowest = Color(0x14FFFFFF),
    surfaceContainerLow = Color(0x1AFFFFFF),
    surfaceContainer = Color(0x22FFFFFF),
    surfaceContainerHigh = Color(0x2CFFFFFF),
    surfaceContainerHighest = Color(0x38FFFFFF),
    outline = Color(0x42FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
)

/** True when [mode] resolves to the dark scheme for the current system setting. */
@Composable
private fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Applies the whole MechForge design system: colour scheme, typography, shapes,
 * semantic state colours, UI strings and layout direction.
 *
 * `language` drives both the strings and the layout direction (Part C3): when the
 * resolved language is Arabic the entire tree is mirrored to RTL, which is why
 * every screen must use `start`/`end` padding and AutoMirrored icons.
 */
@Composable
fun MechForgeTheme(
    mode: ThemeMode,
    language: LanguageMode = LanguageMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = isDark(mode)
    val strings = resolveStrings(language)
    val colors = if (dark) DarkColors else LightColors
    val mechColors = if (dark) DarkMechForgeColors else LightMechForgeColors
    val layoutDirection = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalMechForgeColors provides mechColors,
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = MechForgeTypography,
            shapes = MechForgeShapes,
            content = content,
        )
    }
}
