package com.mechforge.core.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/** Fixed-decimal formatting without java.text so `core` stays pure Kotlin. */
object Fmt {

    fun n(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        val negative = value < 0
        val v = abs(value)
        val factor = 10.0.pow(decimals)
        val scaled = round(v * factor)
        val intPart = (scaled / factor).toLong()
        val fracPart = (scaled - intPart * factor).toLong()
        var fracStr = if (decimals > 0) fracPart.toString().padStart(decimals, '0') else ""
        var intStr = intPart.toString()
        if (decimals > 0 && fracStr.length > decimals) {
            // rounding carried into the integer part (e.g. 0.999 -> 1.00)
            intStr = (intPart + 1).toString()
            fracStr = fracStr.substring(1)
        }
        return buildString {
            if (negative && (intPart != 0L || fracPart != 0L)) append('-')
            append(intStr)
            if (decimals > 0) {
                append('.')
                append(fracStr)
            }
        }
    }
}
