package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Air velocity in a rectangular duct: v = Q/(W·H). */
private val Def = CalculatorDefinition(
    id = "duct-velocity",
    name = "Duct Air Velocity",
    category = CalculatorCategory.HVAC,
    description = "Mean air velocity in a rectangular duct from airflow and duct dimensions.",
    formulaDisplay = "v = Q / (W·H)",
    reference = "Continuity equation applied to duct flow (ASHRAE practice for velocity limits).",
    notes = "Typical supply duct velocities: 4–8 m/s in main ducts, 2–5 m/s in branches (practice ranges — verify against noise criteria).",
    keywords = listOf("duct", "velocity", "air", "hvac", "rectangular"),
    inputs = listOf(
        InputSpec("q", "Airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("w", "Duct width", "W", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("h", "Duct height", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object DuctVelocityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val w = value(inputs, "w")
        val h = value(inputs, "h")

        val area = w * h
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
                "Duct area: A = W·H = ${Fmt.n(w, 4)} × ${Fmt.n(h, 4)} = ${Fmt.n(area, 5)} m²",
                "Velocity: v = Q/A = ${Fmt.n(q, 5)} / ${Fmt.n(area, 5)} = ${Fmt.n(v, 3)} m/s (${Fmt.n(v / 0.00508, 0)} ft/min)",
            ),
            warnings = warnings,
        )
    }
}
