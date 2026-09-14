package com.mechforge.core.math

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Darcy friction factor (Colebrook-White). Laminar: 64/Re exact.
 * Turbulent: iterative solution of
 *     1/√f = -2·log10( ε/(3.7·D) + 2.51/(Re·√f) )
 * converged to a relative tolerance of 1e-12, seeded with Swamee-Jain.
 */
object FrictionFactor {

    const val LAMINAR_LIMIT = 2300.0
    const val TURBULENT_LIMIT = 4000.0

    fun darcy(re: Double, relativeRoughness: Double): Double {
        require(re > 0.0) { "Reynolds number must be positive." }
        require(relativeRoughness >= 0.0) { "Relative roughness must not be negative." }
        if (re < LAMINAR_LIMIT) return 64.0 / re

        var f = swameeJain(re, relativeRoughness)
        repeat(200) {
            val sqrtF = sqrt(f)
            val rhs = -2.0 * log10(relativeRoughness / 3.7 + 2.51 / (re * sqrtF))
            val next = 1.0 / (rhs * rhs)
            val delta = abs(next - f)
            f = next
            if (delta < 1e-12 * f) return f
        }
        return f
    }

    fun swameeJain(re: Double, relativeRoughness: Double): Double =
        0.25 / log10(relativeRoughness / 3.7 + 5.74 / re.pow(0.9)).pow(2)

    fun regimeName(re: Double): String = when {
        re < LAMINAR_LIMIT -> "laminar"
        re <= TURBULENT_LIMIT -> "transitional"
        else -> "turbulent"
    }

    fun regimeWarning(re: Double): String? = when {
        re < LAMINAR_LIMIT -> null
        re <= TURBULENT_LIMIT ->
            "Reynolds number is in the transitional regime (2300–4000); " +
                "the friction factor is uncertain there — treat the result with caution."
        else -> null
    }
}
