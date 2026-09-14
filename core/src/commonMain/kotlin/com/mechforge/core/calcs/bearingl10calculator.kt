package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.pow

/** Rolling bearing rating life: L10 = (C/P)^p · 10⁶ revolutions. */
private val Def = CalculatorDefinition(
    id = "bearing-l10",
    name = "Bearing L10 Life",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Basic rating life of a rolling bearing from dynamic load rating and equivalent load, in revolutions and hours.",
    formulaDisplay = "L10 = (C/P)^p · 10⁶ rev ;  L10h = 10⁶/(60·n)·(C/P)^p",
    reference = "ISO 281 basic rating life (L10); p = 3 for ball bearings, 10/3 for roller bearings.",
    notes = "This is the BASIC rating life (90% reliability) without the ISO 281 life-modification factor (a_ISO). Verify with the bearing manufacturer for the actual application.",
    keywords = listOf("bearing", "l10", "life", "iso 281", "dynamic load", "ball", "roller"),
    inputs = listOf(
        InputSpec("c", "Dynamic load rating", "C", UnitFamily.FORCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("p", "Equivalent dynamic load", "P", UnitFamily.FORCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("exp", "Life exponent p", "p", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("n", "Rotational speed", "n", UnitFamily.ROTATIONAL_SPEED, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
    ),
)

object BearingL10Calculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val c = value(inputs, "c")
        val p = value(inputs, "p")
        val exponent = optionalValue(inputs, "exp", 3.0)

        val ratio = (c / p).pow(exponent)
        val l10Revs = ratio * 1e6 // revolutions

        val results = mutableListOf(
            result("l10", "Basic Rating Life (revolutions)", l10Revs, "rev", isPrimary = true),
        )
        val steps = mutableListOf(
            "Load ratio: C/P = ${Fmt.n(c / 1000.0, 3)} kN / ${Fmt.n(p / 1000.0, 3)} kN = ${Fmt.n(c / p, 3)}",
            "L10 = (C/P)^p · 10⁶ = ${Fmt.n(c / p, 3)}^${Fmt.n(exponent, 2)} × 10⁶ = ${Fmt.n(l10Revs / 1e6, 1)} × 10⁶ revolutions",
        )

        if (has(inputs, "n")) {
            val n = value(inputs, "n")
            val hours = l10Revs / (60.0 * n)
            results += result("l10h", "Life in Hours", hours, "h", isPrimary = true)
            steps += "L10h = L10/(60·n) = ${Fmt.n(l10Revs / 1e6, 1)}×10⁶ / (60 × ${Fmt.n(n, 0)}) = ${Fmt.n(hours, 0)} h"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "exp")) add("Exponent not provided — assumed p = 3 (ball bearings). Use 10/3 for roller bearings.")
                if (!has(inputs, "n")) add("Speed not provided — hours-based life not computed.")
            },
        )
    }
}
