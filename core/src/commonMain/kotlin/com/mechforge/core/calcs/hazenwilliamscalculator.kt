package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
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
    formulaDisplay = "Q = 0.2785·C·D^2.63·S^0.54 ;  h_f = 10.67·L·Q^1.852 / (C^1.852·D^4.87)",
    reference = "Hazen-Williams (1905); SI coefficients as standardised in hydraulic practice.",
    notes = "For water at ordinary temperatures. C: ~140 new PVC/HDPE, ~130 new steel/CI, ~100 old steel (reference values — verify for your pipe).",
    keywords = listOf("hazen williams", "water main", "friction loss", "C factor", "pipeline"),
    inputs = listOf(
        InputSpec("c", "Hazen-Williams C", "C", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("s", "Hydraulic slope", "S", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("q", "Discharge (to size the head loss instead)", "Q", UnitFamily.FLOW, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("l", "Pipe length (with the discharge)", "L", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
    ),
)

object HazenWilliamsCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val c = value(inputs, "c")
        val d = value(inputs, "d")
        val sGiven = has(inputs, "s")
        val qGiven = has(inputs, "q") && has(inputs, "l")
        if (sGiven == qGiven) {
            throw ValidationException(
                listOf(
                    InputError(
                        "s",
                        "Enter either the hydraulic slope S, or both the discharge Q and the length L - not both routes and not neither.",
                    ),
                ),
            )
        }

        // 0.2785 is the SI coefficient consistent with the 10.67 used in the head-loss form; the older
        // 0.278 rounding disagreed with it by about 0.2 %, which is small but is still an inconsistency
        // inside the same calculator.
        val q = if (sGiven) 0.2785 * c * d.pow(2.63) * value(inputs, "s").pow(0.54) else value(inputs, "q") // m³/s
        val area = PI * d * d / 4.0
        val v = q / area

        val l = if (sGiven) 1000.0 else value(inputs, "l")
        val hf1000 = 10.67 * l * q.pow(1.852) / (c.pow(1.852) * d.pow(4.87))
        // The slope actually realised: for the S route it is the entered slope, for the Q route it
        // comes out of the head loss over the given length.
        val s = if (sGiven) value(inputs, "s") else hf1000 / l

        return CalcOutput(
            results = listOf(
                result("q", "Discharge", q * 3600.0, "m3h", isPrimary = true),
                result("v", "Velocity", v, "ms", isPrimary = true),
                result("hf", "Head Loss over the length", hf1000, "m"),
                result("gradient", "Hydraulic Gradient", hf1000 / 1000.0, "dash"),
            ),
            steps = listOf(
                "D^2.63 = ${Fmt.n(d, 4)}^2.63 = ${Fmt.n(d.pow(2.63), 6)}",
                "S^0.54 = ${Fmt.n(s, 5)}^0.54 = ${Fmt.n(s.pow(0.54), 6)}",
                if (sGiven) {
                    "Discharge: Q = 0.2785·C·D^2.63·S^0.54 = 0.2785 × ${Fmt.n(c, 0)} × ${Fmt.n(d.pow(2.63), 6)} × ${Fmt.n(value(inputs, "s").pow(0.54), 6)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 2)} m³/h"
                } else {
                    "Discharge entered: Q = ${Fmt.n(q * 3600.0, 2)} m³/h (${Fmt.n(q, 6)} m³/s) over L = ${Fmt.n(l, 1)} m"
                },
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
                if (sGiven) {
                    "التصرف: Q = 0.2785·C·D^2.63·S^0.54 = 0.2785 × ${Fmt.n(c, 0)} × ${Fmt.n(d.pow(2.63), 6)} × ${Fmt.n(value(inputs, "s").pow(0.54), 6)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 2)} m³/h"
                } else {
                    "التصرف مُدخل: Q = ${Fmt.n(q * 3600.0, 2)} m³/h (${Fmt.n(q, 6)} m³/s) على طول L = ${Fmt.n(l, 1)} m"
                },
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
