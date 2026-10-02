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
import kotlin.math.pow

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
        if (hasRound == hasRect || (hasRound && (has(inputs, "w") || has(inputs, "h"))) || (has(inputs, "w") != has(inputs, "h"))) {
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

        // Friction-equivalent diameter (ASHRAE): the round duct with the same pressure gradient.
        // It is not the same as the equal-area diameter, which is larger and loses less than a
        // rectangular duct of the same area really does.
        val de = if (hasRound) {
            value(inputs, "d")
        } else {
            val a = value(inputs, "w")
            val b = value(inputs, "h")
            1.30 * (a * b).pow(0.625) / (a + b).pow(0.25)
        }
        val aspect = if (hasRound) {
            1.0
        } else {
            maxOf(value(inputs, "w"), value(inputs, "h")) / minOf(value(inputs, "w"), value(inputs, "h"))
        }

        val warnings = buildList {
            if (v > 8.0) add("Velocity above 8 m/s — likely noise and high pressure loss in occupied spaces.")
            if (v < 2.0) add("Velocity below 2 m/s — duct is oversized for a main run; check space constraints vs cost.")
            if (aspect > 4.0) add("Aspect ratio W/H = ${Fmt.n(aspect, 2)} is above the 4:1 that practice recommends: friction and fitting losses grow quickly as a duct gets flat, and the equal-area diameter flatters it.")
        }

        return CalcOutput(
            results = listOf(
                result("v", "Air Velocity", v, "ms", isPrimary = true),
                result("vFpm", "Air Velocity (imperial)", v / 0.00508, "fpm"),
                result("a", "Duct Area", area, "m2"),
                result("de", "Friction-Equivalent Round Diameter (ASHRAE)", de * 1000.0, "mm", isRecommended = true),
                result("aspect", "Aspect Ratio W/H", aspect, "dash"),
            ),
            steps = listOf(
                if (hasRound) {
                    "Round duct area: A = PI*D^2/4 = PI x ${Fmt.n(value(inputs, "d"), 4)}^2 / 4 = ${Fmt.n(area, 5)} m2"
                } else {
                    "Rectangular duct area: A = W*H = ${Fmt.n(value(inputs, "w"), 4)} x ${Fmt.n(value(inputs, "h"), 4)} = ${Fmt.n(area, 5)} m2"
                },
                "Velocity: v = Q/A = ${Fmt.n(q, 5)} / ${Fmt.n(area, 5)} = ${Fmt.n(v, 3)} m/s (${Fmt.n(v / 0.00508, 0)} ft/min)",
                if (hasRound) {
                    "Friction-equivalent diameter = the duct diameter itself: ${Fmt.n(de * 1000.0, 1)} mm"
                } else {
                    "Friction-equivalent diameter (ASHRAE) = 1.30 x (W*H)^0.625 / (W+H)^0.25 = 1.30 x (${Fmt.n(value(inputs, "w") * 1000.0, 0)} x ${Fmt.n(value(inputs, "h") * 1000.0, 0)})^0.625 / (${Fmt.n((value(inputs, "w") + value(inputs, "h")) * 1000.0, 0)})^0.25 = ${Fmt.n(de * 1000.0, 1)} mm - not the equal-area diameter"
                },
                "Aspect ratio W/H = ${Fmt.n(aspect, 2)} (practice prefers 4:1 or flatter sections are avoided)",
            ),
            warnings = warnings,
            stepsAr = listOf(
                if (hasRound) {
                    "مساحة الدكت الدائري: A = π·D²/4 = π × ${Fmt.n(value(inputs, "d"), 4)}² / 4 = ${Fmt.n(area, 5)} m2"
                } else {
                    "مساحة الدكت المستطيل: A = W×H = ${Fmt.n(value(inputs, "w"), 4)} × ${Fmt.n(value(inputs, "h"), 4)} = ${Fmt.n(area, 5)} m2"
                },
                "السرعة: v = Q/A = ${Fmt.n(q, 5)} / ${Fmt.n(area, 5)} = ${Fmt.n(v, 3)} m/s (${Fmt.n(v / 0.00508, 0)} ft/min)",
                if (hasRound) {
                    "قطر الاحتكاك المكافئ = قطر الدكت نفسه: ${Fmt.n(de * 1000.0, 1)} mm"
                } else {
                    "قطر الاحتكاك المكافئ (ASHRAE) = 1.30 × (W×H)^0.625 / (W+H)^0.25 = 1.30 × (${Fmt.n(value(inputs, "w") * 1000.0, 0)} × ${Fmt.n(value(inputs, "h") * 1000.0, 0)})^0.625 / (${Fmt.n((value(inputs, "w") + value(inputs, "h")) * 1000.0, 0)})^0.25 = ${Fmt.n(de * 1000.0, 1)} mm - وليس قطر المساحة المتساوية"
                },
                "نسبة الأبعاد W/H = ${Fmt.n(aspect, 2)} (الممارسة تُفضّل 4:1 أو أقل)",
            ),
            warningsAr = buildList {
                if (v > 8.0) {
                    add("سرعة أعلى من 8 m/s - الضوضاء وفقد الضغط مرتفعان في الفراغات المأهولة.")
                }
                if (v < 2.0) {
                    add("سرعة أقل من 2 m/s - الدكت أكبر من اللازم لمسار رئيسي؛ راجع قيود المساحة مقابل التكلفة.")
                }
                if (aspect > 4.0) {
                    add("نسبة الأبعاد W/H = ${Fmt.n(aspect, 2)} أعلى من 4:1 الموصى بها: فقد الاحتكاك والوصلات يزيد سريعًا مع تفلطح الدكت، وقطر المساحة المتساوية يجمّله.")
                }
            },
        )
    }
}
