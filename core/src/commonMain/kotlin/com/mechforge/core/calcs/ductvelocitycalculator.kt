package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.util.Fmt
import kotlin.math.PI

/** Air velocity in a rectangular duct: v = Q/(W·H). */
private val Def = CalculatorDefinition(
    id = "duct-velocity",
    name = "Duct Air Velocity",
    category = CalculatorCategory.HVAC,
    description = "Mean air velocity in a round or rectangular duct from the airflow and the duct dimensions.",
    formulaDisplay = "v = Q / A   ;   A = W*H (rectangular) or PI*D^2/4 (round)",
    reference = "Continuity equation applied to duct flow (ASHRAE practice for velocity limits).",
    notes = "Typical supply duct velocities: 4–8 m/s in main ducts, 2–5 m/s in branches (practice ranges — verify against noise criteria).",
    keywords = listOf("duct", "velocity", "air", "hvac", "rectangular"),
    inputs = listOf(
        InputSpec("q", "Airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("d", "Round duct diameter", "D", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("w", "Rectangular duct width", "W", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("h", "Rectangular duct height", "H", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object DuctVelocityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")

        val hasRound = has(inputs, "d")
        val hasRect = has(inputs, "w") && has(inputs, "h")
        if (hasRound == hasRect) {
            throw ValidationException(
                listOf(
                    InputError(
                        "d",
                        "Enter either the round duct diameter, OR both the width and the height of a rectangular duct.",
                    ),
                ),
            )
        }

        val area = if (hasRound) {
            val diameter = value(inputs, "d")
            PI * diameter * diameter / 4.0
        } else {
            value(inputs, "w") * value(inputs, "h")
        }
        val v = q / area

        val warnings = buildList {
            if (v > 8.0) add("Velocity above 8 m/s — likely noise and high pressure loss in occupied spaces.")
            if (v < 2.0) add("Velocity below 2 m/s — duct is oversized for a main run; check space constraints vs cost.")
        }

        return CalcOutput(
            results = listOf(
                result("v", "Air Velocity", v, "ms", isPrimary = true),
                result("vFpm", "Air Velocity (imperial)", v / 0.00508, "fpm"),
                result("a", "Duct Area", area, "m2"),
            ),
            steps = listOf(
                if (hasRound) {
                    "Round duct area: A = PI*D^2/4 = PI x ${Fmt.n(value(inputs, "d"), 4)}^2 / 4 = ${Fmt.n(area, 5)} m2"
                } else {
                    "Rectangular duct area: A = W*H = ${Fmt.n(value(inputs, "w"), 4)} x ${Fmt.n(value(inputs, "h"), 4)} = ${Fmt.n(area, 5)} m2"
                },
                "Velocity: v = Q/A = ${Fmt.n(q, 5)} / ${Fmt.n(area, 5)} = ${Fmt.n(v, 3)} m/s (${Fmt.n(v / 0.00508, 0)} ft/min)",
            ),
            warnings = warnings,
            stepsAr = listOf(
                if (hasRound) {
                    "مساحة الدكت الدائري: A = π·D²/4 = π × ${Fmt.n(value(inputs, "d"), 4)}² / 4 = ${Fmt.n(area, 5)} m2"
                } else {
                    "مساحة الدكت المستطيل: A = W×H = ${Fmt.n(value(inputs, "w"), 4)} × ${Fmt.n(value(inputs, "h"), 4)} = ${Fmt.n(area, 5)} m2"
                },
                "السرعة: v = Q/A = ${Fmt.n(q, 5)} / ${Fmt.n(area, 5)} = ${Fmt.n(v, 3)} m/s (${Fmt.n(v / 0.00508, 0)} ft/min)",
            ),
            warningsAr = buildList {
                if (v > 8.0) {
                    add("سرعة أعلى من 8 m/s - الضوضاء وفقد الضغط مرتفعان في الفراغات المأهولة.")
                }
                if (v < 2.0) {
                    add("سرعة أقل من 2 m/s - الدكت أكبر من اللازم لمسار رئيسي؛ راجع قيود المساحة مقابل التكلفة.")
                }
            },
        )
    }
}
