package com.mechforge.app.ui.util

import com.mechforge.core.util.Fmt
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * UI-side number formatter (Part A7).
 *
 * `core`'s [Fmt] is deliberately dumb: fixed decimal places, no scientific
 * notation, no locale. That is exactly right for the engine's own error text, but
 * as a *display* format it loses information — `Fmt.n(1.2e-9, 4)` prints
 * `0.0000`, which is indistinguishable from zero, and friction factors, leak rates
 * and thermal expansions in the millimetre/micron range hit that path constantly.
 *
 * So the engine keeps [Fmt] and the UI gets this formatter instead:
 *
 *  - **significant-figure aware** in the normal range: the number of decimals
 *    adapts so the value always carries ~6 significant figures, then trailing
 *    zeros are trimmed (`0.500000` → `0.5`, `1234.5678` → `1234.57`);
 *  - **scientific notation** outside a sensible magnitude band — at or above 1e7
 *    and at or below 1e-5 — written as `1.23457e+09` / `5.5e-07`;
 *  - locale-free (a decimal point, never a comma), so exports and screenshots are
 *    stable and the report can be re-parsed by a script.
 *
 * It rounds for *display only* and never feeds a value back into the engine: the
 * engine always receives the raw doubles from the input fields.
 */
object UiFormat {

    /** Default significant figures for on-screen and exported values. */
    const val DEFAULT_SIG_FIGS = 6

    /** Fixed notation is used for exponents in (-5, 7); outside it, scientific. */
    private const val SCIENTIFIC_MIN_EXPONENT = 7
    private const val SCIENTIFIC_MAX_EXPONENT = -5

    /** Upper bound on printed decimals; keeps pathological values from filling the screen. */
    private const val MAX_DECIMALS = 10

    /**
     * Formats [value] for display. `NaN`/`Infinity` render as readable words rather
     * than `NaN`/`Infinity`, so a failed calculation cannot be mistaken for a number.
     */
    fun n(value: Double, sigFigs: Int = DEFAULT_SIG_FIGS): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0.0) "∞" else "−∞"
        if (value == 0.0) return "0"
        require(sigFigs >= 1) { "sigFigs must be >= 1" }

        val magnitude = abs(value)
        val exponent = floor(log10(magnitude)).toInt()
        val body = if (exponent >= SCIENTIFIC_MIN_EXPONENT || exponent <= SCIENTIFIC_MAX_EXPONENT) {
            scientific(magnitude, exponent, sigFigs)
        } else {
            fixed(magnitude, exponent, sigFigs)
        }
        return if (value < 0.0) "-$body" else body
    }

    private fun fixed(magnitude: Double, exponent: Int, sigFigs: Int): String {
        val decimals = (sigFigs - exponent - 1).coerceIn(0, MAX_DECIMALS)
        return trimTrailingZeros(Fmt.n(magnitude, decimals))
    }

    private fun scientific(magnitude: Double, exponent: Int, sigFigs: Int): String {
        val mantissa = magnitude / pow10(exponent)
        // One digit before the point, so the mantissa carries sigFigs-1 decimals.
        val decimals = (sigFigs - 1).coerceIn(0, MAX_DECIMALS)
        val mantissaText = trimTrailingZeros(Fmt.n(mantissa, decimals))
        val sign = if (exponent < 0) "-" else "+"
        val digits = abs(exponent).toString().padStart(2, '0')
        return "${mantissaText}e$sign$digits"
    }

    /** Locale-free power of ten (no `java.math` so this stays platform-independent). */
    private fun pow10(exponent: Int): Double {
        var result = 1.0
        var remaining = abs(exponent)
        var base = 10.0
        while (remaining > 0) {
            if (remaining and 1 == 1) result *= base
            base *= base
            remaining = remaining shr 1
        }
        return if (exponent < 0) 1.0 / result else result
    }

    private fun trimTrailingZeros(text: String): String {
        if (!text.contains('.')) return text
        val trimmed = text.trimEnd('0')
        return when {
            trimmed.endsWith('.') -> trimmed.dropLast(1)
            trimmed.isEmpty() -> "0"
            else -> trimmed
        }
    }
}
