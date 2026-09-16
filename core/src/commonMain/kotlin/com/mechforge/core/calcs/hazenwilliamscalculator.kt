package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.PI
import kotlin.math.pow

/** Hazen-Williams flow and head loss for water mains (SI form). */
private val Def = CalculatorDefinition(
    id = "hazen-williams",
    name = "Hazen-Williams (Water Mains)",
    category = CalculatorCategory.HYDRAULICS,
    description = "Discharge and friction head loss in a full circular pipe using the Hazen-Williams formula.",
    formulaDisplay = "Q = 0.278·C·D^2.63·S^0.54 ;  h_f = 10.67·L·Q^1.852 / (C^1.852·D^4.87)",
    reference = "Hazen-Williams (1905); SI coefficients as standardised in hydraulic practice.",
    notes = "For water at ordinary temperatures. C: ~140 new PVC/HDPE, ~130 new steel/CI, ~100 old steel (reference values — verify for your pipe).",
    keywords = listOf("hazen williams", "water main", "friction loss", "C factor", "pipeline"),
    inputs = listOf(
        InputSpec("c", "Hazen-Williams C", "C", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("s", "Hydraulic slope", "S", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object HazenWilliamsCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val c = value(inputs, "c")
        val d = value(inputs, "d")
        val s = value(inputs, "s")

        val q = 0.278 * c * d.pow(2.63) * s.pow(0.54) // m³/s
        val area = PI * d * d / 4.0
        val v = q / area

        val l = 1000.0
        val hf1000 = 10.67 * l * q.pow(1.852) / (c.pow(1.852) * d.pow(4.87))

        return CalcOutput(
            results = listOf(
                result("q", "Discharge", q * 3600.0, "m3h", isPrimary = true),
                result("v", "Velocity", v, "ms", isPrimary = true),
                result("hf", "Head Loss per 1000 m", hf1000, "m"),
                result("gradient", "Hydraulic Gradient", hf1000 / 1000.0, "dash"),
            ),
            steps = listOf(
                "D^2.63 = ${Fmt.n(d, 4)}^2.63 = ${Fmt.n(d.pow(2.63), 6)}",
                "S^0.54 = ${Fmt.n(s, 5)}^0.54 = ${Fmt.n(s.pow(0.54), 6)}",
                "Discharge: Q = 0.278·C·D^2.63·S^0.54 = 0.278 × ${Fmt.n(c, 0)} × ${Fmt.n(d.pow(2.63), 6)} × ${Fmt.n(s.pow(0.54), 6)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 2)} m³/h",
                "Velocity: v = Q/A = ${Fmt.n(q, 6)} / ${Fmt.n(area, 6)} = ${Fmt.n(v, 3)} m/s",
                "Head loss (L = 1000 m): h_f = 10.67·L·Q^1.852/(C^1.852·D^4.87) = ${Fmt.n(hf1000, 3)} m",
            ),
            warnings = buildList {
                if (v > 3.0) add("Velocity above 3 m/s — review surge/erosion and pressure class.")
                if (v < 0.6) add("Velocity below 0.6 m/s — sedimentation risk in raw-water mains.")
            },
            stepsAr = listOf(
                "D^2.63 = ${Fmt.n(d, 4)}^2.63 = ${Fmt.n(d.pow(2.63), 6)}",
                "S^0.54 = ${Fmt.n(s, 5)}^0.54 = ${Fmt.n(s.pow(0.54), 6)}",
                "التصرف: Q = 0.278·C·D^2.63·S^0.54 = 0.278 × ${Fmt.n(c, 0)} × ${Fmt.n(d.pow(2.63), 6)} × ${Fmt.n(s.pow(0.54), 6)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 2)} m³/h",
                "السرعة: v = Q/A = ${Fmt.n(q, 6)} / ${Fmt.n(area, 6)} = ${Fmt.n(v, 3)} m/s",
                "فقد الرفع (L = 1000 m): h_f = 10.67·L·Q^1.852/(C^1.852·D^4.87) = ${Fmt.n(hf1000, 3)} m",
            ),
            warningsAr = buildList {
                if (v > 3.0) {
                    add("سرعة أعلى من 3 m/s - راجع الصدمة والتآكل ودرجة ضغط الماسورة.")
                }
                if (v < 0.6) {
                    add("سرعة أقل من 0.6 m/s - خطر ترسيب في خطوط المياه الخام.")
                }
            },
        )
    }
}
