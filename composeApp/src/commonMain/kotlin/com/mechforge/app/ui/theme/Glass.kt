package com.mechforge.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Glass layer (design pass).
 *
 * The app reads as a set of frosted-glass panes floating over a deep engineering
 * backdrop. Three ingredients, in this order:
 *
 *  1. a layered backdrop (vertical base gradient + two soft light orbs) painted once at
 *     the app shell, so every translucent surface above it has something to refract;
 *  2. translucent surfaces: the Material surface tokens are semi-transparent, so Cards,
 *     text fields, dialogs and menus become glass without any per-screen work;
 *  3. a luminous hairline border on the surfaces that carry content (top bar, drawer,
 *     result blocks), which is what makes the pane edges legible.
 *
 * No blur filter is used on purpose: radial gradients fade to transparent on their own,
 * so the backdrop stays soft on every API level and on the desktop target, and text
 * never sits behind a blurred layer.
 *
 * Brand colours are untouched (EngineeringBlue / Cyan): the glass look comes from
 * translucency, depth and light, not from a new palette.
 */
internal object Glass {

    /** Identity colours reused as the light sources behind the glass. */
    val Blue = Color(0xFF1B4F8A)
    val Cyan = Color(0xFF00A5C4)

    // Backdrop base, dark and light.
    private val DarkBase = listOf(Color(0xFF05070D), Color(0xFF0A1220), Color(0xFF04060B))
    private val LightBase = listOf(Color(0xFFE8EEFA), Color(0xFFF7F9FD), Color(0xFFEDF1F9))

    /** True when the active colour scheme is the dark one. */
    @Composable
    fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.35f

    /** Panel fill: translucent white, stronger in light mode where white reads as glass. */
    @Composable
    fun fill(): Color =
        if (isDark()) Color(0xFF0E1626).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.82f)

    /** Fill for surfaces that carry content (result blocks, dialogs, menu sheets). */
    @Composable
    fun fillStrong(): Color =
        if (isDark()) Color(0xFF131D31).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.92f)

    /** Fill for chrome (bars, drawer, sidebar) - a touch denser so text stays legible. */
    @Composable
    fun fillChrome(): Color =
        if (isDark()) Color(0xFF0B1220).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.88f)

    /** Hairline highlight that catches the light along the top-left edge of a pane. */
    @Composable
    fun border(): Brush = if (isDark()) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f)),
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 1.00f), Color.White.copy(alpha = 0.55f)),
        )
    }

    /** Backdrop brush for the app shell. */
    @Composable
    fun backdrop(): Brush =
        Brush.verticalGradient(if (isDark()) DarkBase else LightBase)
}

/**
 * App-shell backdrop: base gradient plus two soft light orbs. Wrap the whole navigation
 * shell in this once; screens stay transparent and let it show through.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val dark = Glass.isDark()
    Box(modifier.fillMaxSize().background(Glass.backdrop())) {
        // Light orbs. Offsets are negative on purpose: the panes overlap the bright edge.
        Box(
            Modifier
                .offset(x = (-140).dp, y = (-180).dp)
                .size(520.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Glass.Blue.copy(alpha = if (dark) 0.34f else 0.16f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .offset(x = 190.dp, y = 420.dp)
                .size(460.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Glass.Cyan.copy(alpha = if (dark) 0.20f else 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/**
 * Turns any surface into a glass pane: translucent fill, luminous hairline border, and
 * the theme's corner radius. [strong] is for content-bearing blocks (results, dialogs).
 */
@Composable
fun Modifier.glassPanel(
    shape: Shape = MaterialTheme.shapes.large,
    strong: Boolean = false,
): Modifier {
    val fill = if (strong) Glass.fillStrong() else Glass.fill()
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(fill, fill.copy(alpha = fill.alpha * 0.55f)),
            ),
        )
        .border(width = 1.dp, brush = Glass.border(), shape = shape)
}

/** Glass variant for the navigation chrome (bars, drawer, sidebar). */
@Composable
fun Modifier.glassChrome(shape: Shape = MaterialTheme.shapes.extraSmall): Modifier {
    val fill = Glass.fillChrome()
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(listOf(fill, fill.copy(alpha = fill.alpha * 0.70f))),
        )
        .border(width = 1.dp, brush = Glass.border(), shape = shape)
}

/**
 * Hairline highlight only, for surfaces that already paint their own translucent fill
 * (Material cards, sheets). Keeps every pane edge legible without stacking fills.
 */
@Composable
fun Modifier.glassBorder(shape: Shape = MaterialTheme.shapes.medium): Modifier =
    this.border(width = 1.dp, brush = Glass.border(), shape = shape)
