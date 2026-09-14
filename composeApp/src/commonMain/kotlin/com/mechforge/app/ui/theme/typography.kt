package com.mechforge.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MechForge type scale (README v2 §29). The stock Material 3 scale is anonymous:
 * every step sits at the same weight and letter-spacing, so nothing reads as a
 * heading. Here:
 *
 *  - headings/display steps get a real weight step (SemiBold) and *negative*
 *    letter-spacing — large type set at default tracking looks loose;
 *  - body steps keep a small positive tracking for readability;
 *  - labels are Medium weight so they stay legible at 11–14sp inside chips and
 *    field labels;
 *  - display sizes are reduced versus Material defaults (57sp is a marketing
 *    headline, not an engineering tool) which also helps when the UI is mirrored
 *    into Arabic, whose glyphs are wider.
 */
val MechForgeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.75).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Monospaced style for the headline result value: digits keep their column when the
 * value is recalculated or the unit changes, which is what makes a result readable
 * at a glance. Not part of [Typography] (which has a fixed set of 15 steps), so it
 * is exposed separately and used only for the primary result number.
 */
val ResultNumberStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp,
)

/** Monospaced style for secondary result numbers and the converter table. */
val ResultNumberStyleSmall = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp,
)

/**
 * MechForge shape scale. Cards and dialogs are slightly rounder than the Material
 * default so surfaces read as distinct blocks; buttons keep a moderate radius.
 */
val MechForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6),
    small = RoundedCornerShape(10),
    medium = RoundedCornerShape(14),
    large = RoundedCornerShape(18),
    extraLarge = RoundedCornerShape(26),
)
