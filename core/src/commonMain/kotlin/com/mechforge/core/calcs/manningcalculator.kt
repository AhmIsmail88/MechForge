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

/** Manning equation for open-channel / gravity flow: v = (1/n)·R^(2/3)·S^(1/2). */
private val Def = CalculatorDefinition(
    id = "manning",
    name = "Manning Equation (Open Channel)",
    category = CalculatorCategory.HYDRAULICS,
    description = "Mean velocity and discharge in an open channel or gravity pipe from Manning roughness, hydraulic radius and slope.",
    formulaDisplay = "v = (1/n)·R^(2/3)·S^(1/2) ;  Q = A·v",
    reference = "Robert Manning (1891); standard open-channel hydraulics (e.g. Chow, Open-Channel Hydraulics).",
    notes = "SI form. R = A/P (hydraulic radius), S = energy slope (m/m). Typical n: 0.013 concrete, 0.015–0.025 earth channels (reference values — verify for your lining).",
    keywords = listOf("manning", "open channel", "gravity", "sewer", "roughness", "slope"),
    inputs = listOf(
        InputSpec("n", "Manning roughness", "n", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("r", "Hydraulic radius", "R", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("s", "Energy slope", "S", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("a", "Flow area", "A", UnitFamily.AREA, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m2"),
    ),
)

object ManningCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val n = value(inputs, "n")
        val r = value(inputs, "r")
        val s = value(inputs, "s")
        val a = value(inputs, "a")

        val v = (1.0 / n) * r.pow(2.0 / 3.0) * s.pow(0.5)
        val q = a * v

        return CalcOutput(
            results = listOf(
                result("v", "Mean Velocity", v, "ms", isPrimary = true),
                result("q", "Discharge", q * 3600.0, "m3h", isPrimary = true),
                result("qSi", "Discharge (SI)", q, "m3s"),
            ),
            steps = listOf(
                "R^(2/3) = ${Fmt.n(r, 4)}^(2/3) = ${Fmt.n(r.pow(2.0 / 3.0), 5)}",
                "S^(1/2) = √${Fmt.n(s, 5)} = ${Fmt.n(s.pow(0.5), 5)}",
                "Velocity: v = (1/n)·R^(2/3)·S^(1/2) = (1/${Fmt.n(n, 4)}) × ${Fmt.n(r.pow(2.0 / 3.0), 5)} × ${Fmt.n(s.pow(0.5), 5)} = ${Fmt.n(v, 4)} m/s",
                "Discharge: Q = A·v = ${Fmt.n(a, 5)} × ${Fmt.n(v, 4)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 3)} m³/h",
            ),
            warnings = if (v < 0.6 && q > 0) listOf("Velocity below 0.6 m/s — check self-cleansing requirements for the design flow.") else emptyList(),
            stepsAr = listOf(
                "R^(2/3) = ${Fmt.n(r, 4)}^(2/3) = ${Fmt.n(r.pow(2.0 / 3.0), 5)}",
                "S^(1/2) = √${Fmt.n(s, 5)} = ${Fmt.n(s.pow(0.5), 5)}",
                "السرعة: v = (1/n)·R^(2/3)·S^(1/2) = (1/${Fmt.n(n, 4)}) × ${Fmt.n(r.pow(2.0 / 3.0), 5)} × ${Fmt.n(s.pow(0.5), 5)} = ${Fmt.n(v, 4)} m/s",
                "التصرف: Q = A·v = ${Fmt.n(a, 5)} × ${Fmt.n(v, 4)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 3)} m³/h",
            ),
            warningsAr = if (v < 0.6 && q > 0) {
                listOf("سرعة أقل من 0.6 m/s - راجع متطلبات التنظيف الذاتي للتصرف التصميمي.")
            } else {
                emptyList()
            },
        )
    }
}
