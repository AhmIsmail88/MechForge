package com.mechforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the palette against text that cannot be read.
 *
 * The dark theme shipped with unreadable section headings: Material only sets the default text
 * colour when it draws a Surface or a Scaffold, and the wide shell draws a Row over the glass
 * backdrop instead, so unstyled Text fell back to black on a near-black background. The theme now
 * provides that colour from the active scheme (see MechForgeTheme).
 *
 * This test covers the palette half of the problem: it composites each translucent container over
 * the backdrop it actually sits on and checks the text colour that the theme hands out against it.
 * It cannot prove the wiring, only that the colours the wiring supplies are legible on both themes.
 */
class ThemeContrastTest {

    // Backdrop endpoints, mirroring the glass backdrop in Glass.kt.
    private val darkBackdrop = Color(0xFF05070D)
    private val lightBackdrop = Color(0xFFE8EEFA)

    private fun over(color: Color, backdrop: Color): Color {
        val a = color.alpha
        return Color(
            color.red * a + backdrop.red * (1f - a),
            color.green * a + backdrop.green * (1f - a),
            color.blue * a + backdrop.blue * (1f - a),
        )
    }

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = over(fg, bg).luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(schemeName: String, backdrop: Color, pairs: List<Triple<String, Color, Color>>) {
        for ((label, fg, bg) in pairs) {
            val r = ratio(fg, bg)
            assertTrue(r >= 4.5, "%s / %s contrast is %.2f:1, below the 4.5:1 minimum".format(schemeName, label, r))
        }
    }

    @Test
    fun darkSchemeTextIsLegibleOnTheGlassBackdrop() {
        val c = DarkColors
        val surfaceContainer = over(c.surfaceContainer, darkBackdrop)
        check(
            "dark",
            darkBackdrop,
            listOf(
                Triple("onBackground", c.onBackground, darkBackdrop),
                Triple("onSurface", c.onSurface, surfaceContainer),
                Triple("onSurfaceVariant", c.onSurfaceVariant, over(c.surfaceVariant, darkBackdrop)),
                Triple("onPrimary", c.onPrimary, c.primary),
                Triple("onPrimaryContainer", c.onPrimaryContainer, c.primaryContainer),
                Triple("onTertiary", c.onTertiary, c.tertiary),
            ),
        )
    }

    @Test
    fun lightSchemeTextIsLegibleOnTheGlassBackdrop() {
        val c = LightColors
        check(
            "light",
            lightBackdrop,
            listOf(
                Triple("onBackground", c.onBackground, lightBackdrop),
                Triple("onSurface", c.onSurface, over(c.surfaceContainer, lightBackdrop)),
                Triple("onSurfaceVariant", c.onSurfaceVariant, over(c.surfaceVariant, lightBackdrop)),
                Triple("onPrimary", c.onPrimary, c.primary),
                Triple("onPrimaryContainer", c.onPrimaryContainer, c.primaryContainer),
            ),
        )
    }

    /**
     * The specific shape of the bug: the colour the theme hands out as the default text colour must
     * be light on the dark theme and dark on the light theme. A black default on the dark theme is
     * exactly what made the headings disappear.
     */
    @Test
    fun theDefaultTextColourFollowsTheScheme() {
        assertTrue(
            DarkColors.onBackground.luminance() > 0.5f,
            "the dark theme's default text colour is dark: it would vanish on the dark backdrop",
        )
        assertTrue(
            LightColors.onBackground.luminance() < 0.2f,
            "the light theme's default text colour is light: it would vanish on the light backdrop",
        )
    }
}
