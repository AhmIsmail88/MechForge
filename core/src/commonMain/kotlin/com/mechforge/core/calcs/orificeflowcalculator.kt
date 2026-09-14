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

/** Discharge through an orifice: Q = Cd·A·√(2gH). */
private val Def = CalculatorDefinition(
    id = "orifice-flow",
    name = "Orifice Flow",
    category = CalculatorCategory.HYDRAULICS,
    description = "Volumetric discharge through a submerged/free orifice from head and orifice size.",
    formulaDisplay = "Q = Cd·A·√(2gH)",
    reference = "Standard orifice (Torricelli) relation; Cd values from hydraulic handbooks.",
    notes = "Cd default 0.62 (sharp-edged orifice, typical). g = 9.80665 m/s². Head H is the piezometric head across the orifice.",
    keywords = listOf("orifice", "discharge", "flow", "torricelli", "cd"),
    inputs = listOf(
        InputSpec("cd", "Discharge coefficient", "Cd", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "dash"),
        InputSpec("d", "Orifice diameter", "d", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("h", "Head across orifice", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
    ),
)

object OrificeFlowCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val cd = optionalValue(inputs, "cd", 0.62)
        val d = value(inputs, "d")
        val h = value(inputs, "h")

        val area = PI * d * d / 4.0
        val velocity = sqrt(2.0 * G * h)
        val q = cd * area * velocity

        return CalcOutput(
            results = listOf(
                result("q", "Discharge", q * 3600.0, "m3h", isPrimary = true),
                result("qSi", "Discharge (SI)", q, "m3s"),
                result("v", "Theoretical Jet Velocity", velocity, "ms"),
            ),
            steps = listOf(
                "Orifice area: A = π·d²/4 = π × ${Fmt.n(d, 4)}² / 4 = ${Fmt.n(area, 6)} m²",
                "Theoretical velocity: √(2gH) = √(2 × 9.80665 × ${Fmt.n(h, 3)}) = ${Fmt.n(velocity, 4)} m/s",
                "Discharge: Q = Cd·A·√(2gH) = ${Fmt.n(cd, 3)} × ${Fmt.n(area, 6)} × ${Fmt.n(velocity, 4)} = ${Fmt.n(q, 6)} m³/s = ${Fmt.n(q * 3600.0, 3)} m³/h",
            ),
            warnings = if (!has(inputs, "cd")) listOf("Cd not provided — assumed 0.62 (sharp-edged orifice).") else emptyList(),
        )
    }
}
