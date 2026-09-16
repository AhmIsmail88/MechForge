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

/** Cantilever beam with an end point load. */
private val Def = CalculatorDefinition(
    id = "beam-cantilever-point",
    name = "Cantilever Beam — Point Load",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Tip deflection, maximum bending moment and bending stress of a cantilever with a point load at the free end.",
    formulaDisplay = "δ = P·L³/(3·E·I) ;  M_max = P·L ;  σ = M/Z",
    reference = "Standard beam formulae (e.g. Roark's Formulas for Stress and Strain).",
    notes = "Linear-elastic, small-deflection theory. Load is applied at the free end; self-weight is not included.",
    keywords = listOf("cantilever", "beam", "point load", "deflection", "bending", "moment"),
    inputs = listOf(
        InputSpec("p", "Point load", "P", UnitFamily.FORCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("l", "Length", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("e", "Elastic modulus", "E", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("i", "Second moment of area", "I", UnitFamily.SECOND_MOMENT, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm4"),
        InputSpec("z", "Section modulus (optional)", "Z", UnitFamily.SECTION_MODULUS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm3sect"),
    ),
)

object BeamCantileverPointCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p")
        val l = value(inputs, "l")
        val e = value(inputs, "e")
        val i = value(inputs, "i")

        val defl = p * l.pow(3) / (3.0 * e * i)
        val moment = p * l

        val results = mutableListOf(
            result("defl", "Tip Deflection", defl * 1000.0, "mm", isPrimary = true),
            result("moment", "Maximum Bending Moment (at support)", moment, "nm"),
            result("ratio", "Deflection / Length", defl / l, "dash"),
        )
        val stepsAr = mutableListOf<String>()
        val steps = mutableListOf(
            "Flexural rigidity: E·I = ${Fmt.n(e / 1e6, 1)} MPa × ${Fmt.n(i, 12)} m⁴ = ${Fmt.n(e * i / 1000.0, 1)} kN·m²",
            "δ = P·L³/(3·E·I) = ${Fmt.n(p / 1000.0, 3)} kN × ${Fmt.n(l, 2)}³ / (3 × ${Fmt.n(e * i / 1000.0, 1)} kN·m²) = ${Fmt.n(defl * 1000.0, 2)} mm",
            "M_max = P·L = ${Fmt.n(p, 1)} N × ${Fmt.n(l, 2)} m = ${Fmt.n(moment, 1)} N·m",
            "Deflection ratio: L/${Fmt.n(l / defl, 0)}",
        )
        stepsAr += "الصلابة الانثنائية: E·I = ${Fmt.n(e / 1e6, 1)} MPa × ${Fmt.n(i, 12)} m⁴ = ${Fmt.n(e * i / 1000.0, 1)} kN·m²"
        stepsAr += "δ = P·L³/(3·E·I) = ${Fmt.n(p / 1000.0, 3)} kN × ${Fmt.n(l, 2)}³ / (3 × ${Fmt.n(e * i / 1000.0, 1)} kN·m²) = ${Fmt.n(defl * 1000.0, 2)} mm"
        stepsAr += "M_max = P·L = ${Fmt.n(p, 1)} N × ${Fmt.n(l, 2)} m = ${Fmt.n(moment, 1)} N·m"
        stepsAr += "نسبة الانحراف: L/${Fmt.n(l / defl, 0)}"
        if (has(inputs, "z")) {
            val z = value(inputs, "z")
            val sigma = moment / z
            results += result("sigma", "Maximum Bending Stress", sigma / 1e6, "mpa", isPrimary = true)
            steps += "Bending stress: σ = M/Z = ${Fmt.n(moment, 1)} N·m / ${Fmt.n(z * 1e9, 0)} mm³ = ${Fmt.n(sigma / 1e6, 1)} MPa"
            stepsAr += "إجهاد الانحناء: σ = M/Z = ${Fmt.n(moment, 1)} N·m / ${Fmt.n(z * 1e9, 0)} mm³ = ${Fmt.n(sigma / 1e6, 1)} MPa"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            stepsAr = stepsAr,
            warnings = if (!has(inputs, "z")) {
                listOf("Section modulus not provided — bending stress not computed.")
            } else {
                emptyList()
            },
            warningsAr = if (!has(inputs, "z")) {
                listOf("لم يُدخل معامل المقطع — إجهاد الانحناء لم يُحسب.")
            } else {
                emptyList()
            },
        )
    }
}
