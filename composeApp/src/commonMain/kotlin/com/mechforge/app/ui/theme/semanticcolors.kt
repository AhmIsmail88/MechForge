package com.mechforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic state colours (README v2 §29). The brand identity is unchanged —
 * Engineering Blue `#1B4F8A`, container `#D5E4F7`, Graphite `#3C4043`, Cyan
 * `#00A5C4`. These tokens only *add* the states the palette did not have, so a
 * warning stops being painted with the error colour and a success message stops
 * being painted with the primary colour.
 *
 * Hue choices harmonise with the existing palette (same saturation/lightness band
 * as the Material 3 error pair that already ships in [LightColors] / [DarkColors]);
 * the info pair reuses the existing Cyan brand accent.
 */
@Immutable
data class MechForgeColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    /** Foreground for disabled text/icons (Material's 38% content alpha). */
    val disabled: Color,
    /** Container for disabled surfaces. */
    val disabledContainer: Color,
)

internal val LightMechForgeColors = MechForgeColors(
    success = Color(0xFF1E7A46),
    onSuccess = Color.White,
    successContainer = Color(0xFFD7F0E0),
    onSuccessContainer = Color(0xFF07401F),
    warning = Color(0xFF8A5A00),
    onWarning = Color.White,
    warningContainer = Color(0xFFFDEBC8),
    onWarningContainer = Color(0xFF3E2A00),
    info = Cyan,
    onInfo = Color.White,
    infoContainer = Color(0xFFD2F2F8),
    onInfoContainer = Color(0xFF06333D),
    disabled = Color(0xFF43474A).copy(alpha = 0.38f),
    disabledContainer = Color(0xFFECEEF1).copy(alpha = 0.60f),
)

internal val DarkMechForgeColors = MechForgeColors(
    success = Color(0xFF7BD9A5),
    onSuccess = Color(0xFF04301A),
    successContainer = Color(0xFF17452C),
    onSuccessContainer = Color(0xFFBFEBD2),
    warning = Color(0xFFF0C36D),
    onWarning = Color(0xFF3A2A00),
    warningContainer = Color(0xFF4A3406),
    onWarningContainer = Color(0xFFFBE2B0),
    info = Color(0xFF4DD0E1),
    onInfo = Color(0xFF062F35),
    infoContainer = Color(0xFF10414A),
    onInfoContainer = Color(0xFFC7EDF3),
    disabled = Color(0xFFC3C7CB).copy(alpha = 0.38f),
    disabledContainer = Color(0xFF2A2E33).copy(alpha = 0.60f),
)

/** Provided by [MechForgeTheme]; read it through [MaterialTheme.mechColors]. */
val LocalMechForgeColors = staticCompositionLocalOf { LightMechForgeColors }

/** Semantic colours for the current light/dark scheme. */
val MaterialTheme.mechColors: MechForgeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMechForgeColors.current
