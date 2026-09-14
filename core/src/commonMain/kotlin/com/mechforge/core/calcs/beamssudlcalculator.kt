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

/** Simply supported beam, uniformly distributed load. */
private val Def = CalculatorDefinition(
    id = "beam-ss-udl",
    name = "Simply Supported Beam — UDL",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Maximum deflection, bending moment and bending stress of a simply supported beam under a uniform distributed load.",
    formulaDisplay = "δ_max = 5·w·L⁴/(384·E·I) ;  M_max = w·L²/8 ;  σ = M/Z",
    reference = "Standard beam formulae (e.g. Roark's Formulas for Stress and Strain; Shigley).",
    notes = "Linear-elastic, small-deflection theory. w is the load per unit length (service load). Deflection limits (e.g. L/250, L/360) are project/code dependent and not applied here.",
    keywords = listOf("beam", "udl", "deflection", "bending", "simply supported", "moment"),
    inputs = listOf(
        InputSpec("w", "Distributed load", "w", UnitFamily.LINE_LOAD, minValue = 0.0, exclusiveMin = true, defaultUnitId = "knlperm"),
        InputSpec("l", "Span", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("e", "Elastic modulus", "E", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("i", "Second moment of area", "I", UnitFamily.SECOND_MOMENT, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm4"),
        InputSpec("z", "Section modulus (optional)", "Z", UnitFamily.SECTION_MODULUS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm3sect"),
    ),
)

object BeamSsUdlCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val w = value(inputs, "w")
        val l = value(inputs, "l")
        val e = value(inputs, "e")
        val i = value(inputs, "i")

        val ei = e * i
        val defl = 5.0 * w * l.pow(4) / (384.0 * ei)
        val moment = w * l * l / 8.0

        val results = mutableListOf(
            result("defl", "Maximum Deflection", defl * 1000.0, "mm", isPrimary = true),
            result("moment", "Maximum Bending Moment", moment, "nm"),
            result("ratio", "Deflection / Span", defl / l, "dash"),
        )
        val steps = mutableListOf(
            "Flexural rigidity: E·I = ${Fmt.n(e / 1e6, 1)} MPa × ${Fmt.n(i, 12)} m⁴ = ${Fmt.n(ei / 1000.0, 1)} kN·m²",
            "δ_max = 5·w·L⁴/(384·E·I) = 5 × ${Fmt.n(w, 1)} × ${Fmt.n(l, 2)}⁴ / (384 × ${Fmt.n(ei / 1000.0, 1)} kN·m²) = ${Fmt.n(defl * 1000.0, 2)} mm",
            "M_max = w·L²/8 = ${Fmt.n(w, 1)} × ${Fmt.n(l, 2)}² / 8 = ${Fmt.n(moment, 1)} N·m",
            "Deflection ratio: L/${Fmt.n(l / defl, 0)}",
        )
        if (has(inputs, "z")) {
            val z = value(inputs, "z")
            val sigma = moment / z
            results += result("sigma", "Maximum Bending Stress", sigma / 1e6, "mpa", isPrimary = true)
            steps += "Bending stress: σ = M/Z = ${Fmt.n(moment, 1)} N·m / ${Fmt.n(z * 1e9, 0)} mm³ = ${Fmt.n(sigma / 1e6, 1)} MPa"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "z")) add("Section modulus not provided — bending stress not computed.")
                add("Deflection limits (L/250, L/360, etc.) depend on the project specification and are not applied automatically.")
            },
        )
    }
}
