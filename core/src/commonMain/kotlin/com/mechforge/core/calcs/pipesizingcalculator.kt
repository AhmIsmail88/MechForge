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
import kotlin.math.sqrt

/** Velocity-based pipe sizing: D = √(4·Q/(π·v)). */

private val Def = CalculatorDefinition(
    id = "pipe-sizing",
    name = "Pipe Sizing (Velocity-Based)",
    category = CalculatorCategory.PIPING,
    description = "Theoretical internal diameter from flow rate and a chosen design velocity.",
    formulaDisplay = "D = √(4·Q / (π·v))",
    reference = "Continuity equation; velocity limits are engineering practice, not code.",
    notes = "Choose velocity per line role: pump suction 1.2–3.0 m/s (lower preferred), discharge 0.9–2.1 m/s commonly used as a starting point.",
    keywords = listOf("pipe", "sizing", "diameter", "velocity", "design"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("v", "Design velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
    ),
)

object PipeSizingCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val v = value(inputs, "v") // m/s

        val d = sqrt(4.0 * q / (PI * v)) // m
        val dMm = d * 1000.0

        val warnings = buildList {
            if (v > 3.0) add("Selected velocity above 3 m/s — typical water practice is 0.6–3.0 m/s depending on line role.")
            if (v < 0.6) add("Selected velocity below 0.6 m/s — sedimentation and undersizing risk in water lines.")
            add("Result is the theoretical internal diameter; select the next standard pipe size (schedule/series) above it.")
        }

        return CalcOutput(
            results = listOf(
                result("d", "Required Internal Diameter", dMm, "mm", isPrimary = true),
                result("area", "Flow Area", PI * d * d / 4.0, "m2", isPrimary = false),
            ),
            steps = listOf(
                "D = √(4·Q/(π·v)) = √(4 × ${Fmt.n(q, 6)} / (π × ${Fmt.n(v, 3)})) = ${Fmt.n(d, 5)} m = ${Fmt.n(dMm, 1)} mm",
                "Flow area: A = π·D²/4 = ${Fmt.n(PI * d * d / 4.0, 6)} m²",
            ),
            warnings = warnings,
        )
    }

}
