package com.mechforge.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The single spacing scale for the whole UI (README v2 §29). Every screen must use
 * these tokens instead of scattering raw dp literals, so the vertical rhythm is the
 * same on Home, the calculator form and the list screens.
 *
 * Scale: 4 / 8 / 12 / 16 / 24 / 32 dp.
 */
object Spacing {
    /** 4dp — icon-to-text gaps, hairline insets. */
    val xs = 4.dp

    /** 8dp — gap between tightly related elements (label/value, chips). */
    val sm = 8.dp

    /** 12dp — gap between rows of the same group. */
    val md = 12.dp

    /** 16dp — screen gutter, card padding. */
    val lg = 16.dp

    /** 24dp — section break, screen padding on wide layouts. */
    val xl = 24.dp

    /** 32dp — break between the major blocks of a screen. */
    val xxl = 32.dp
}

/** Fixed sizes that are not part of the spacing rhythm. */
object Dimens {
    /** Minimum touch target required for every interactive element. */
    val touchTarget = 48.dp

    /** Narrowest calculator card; the Home grid derives its column count from it. */
    val cardMinWidth = 230.dp

    /** Reading width for long prose (About, reference/notes), keeps line length sane. */
    val proseMaxWidth = 760.dp

    /** Small inline icon (inside a chip, a status line). */
    val iconSm = 18.dp

    val iconMd = 24.dp

    /** Hairline border/divider thickness. */
    val hairline = 1.dp
}
