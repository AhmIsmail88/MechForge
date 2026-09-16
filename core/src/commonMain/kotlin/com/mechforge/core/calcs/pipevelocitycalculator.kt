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

/** Mean pipe flow velocity: v = 4·Q / (π·D²) */

private val Def = CalculatorDefinition(
    id = "pipe-velocity",
    name = "Pipe Flow Velocity",
    category = CalculatorCategory.HYDRAULICS,
    description = "Mean flow velocity inside a circular pipe from flow rate and internal diameter.",
    formulaDisplay = "v = 4·Q / (π·D²)",
    reference = "Continuity equation for incompressible flow.",
    notes = "Typical design velocity for water: 0.6–3.0 m/s (practice, not code).",
    keywords = listOf("velocity", "pipe", "flow", "speed"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object PipeVelocityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val d = value(inputs, "d") // m

        val v = 4.0 * q / (PI * d * d)

        val warnings = buildList {
            if (v > 3.0) add("Velocity above 3 m/s — check erosion, noise and pressure drop for water systems.")
            if (v < 0.3) add("Velocity below 0.3 m/s — sedimentation risk in water lines.")
        }

        return CalcOutput(
            results = listOf(
                result("v", "Flow Velocity", v, "ms", isPrimary = true),
            ),
            steps = listOf(
                "Area: A = π·D²/4 = π × ${Fmt.n(d, 4)}² / 4 = ${Fmt.n(PI * d * d / 4.0, 6)} m²",
                "Velocity: v = Q / A = ${Fmt.n(q, 6)} / ${Fmt.n(PI * d * d / 4.0, 6)} = ${Fmt.n(v, 3)} m/s",
            ),
            warnings = warnings,
            stepsAr = listOf(
                "المساحة: A = π·D²/4 = π × ${Fmt.n(d, 4)}² / 4 = ${Fmt.n(PI * d * d / 4.0, 6)} m²",
                "السرعة: v = Q / A = ${Fmt.n(q, 6)} / ${Fmt.n(PI * d * d / 4.0, 6)} = ${Fmt.n(v, 3)} m/s",
            ),
            warningsAr = buildList {
                if (v > 3.0) {
                    add("سرعة أعلى من 3 m/s - راجع التآكل والضوضاء وفقد الضغط في أنظمة المياه.")
                }
                if (v < 0.3) {
                    add("سرعة أقل من 0.3 m/s - خطر ترسيب في خطوط المياه.")
                }
            },
        )
    }

}
