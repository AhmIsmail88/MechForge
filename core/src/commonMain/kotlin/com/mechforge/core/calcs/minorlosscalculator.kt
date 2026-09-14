package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Minor (local) losses: h_m = K·v²/(2g). */
private val Def = CalculatorDefinition(
    id = "minor-losses",
    name = "Minor Losses (Fittings)",
    category = CalculatorCategory.HYDRAULICS,
    description = "Local head loss through valves, fittings, bends and entrances from the loss coefficient K.",
    formulaDisplay = "h_m = K·v²/(2g)",
    reference = "Standard minor-loss relation; K values from manufacturer data or Crane TP-410 style tables.",
    notes = "g = 9.80665 m/s². K is entered by the user — K tables are reference data and are not embedded.",
    keywords = listOf("minor loss", "fitting", "K", "valve", "bend", "local loss"),
    inputs = listOf(
        InputSpec("k", "Loss coefficient", "K", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("v", "Velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
    ),
)

object MinorLossCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val k = value(inputs, "k")
        val v = value(inputs, "v")
        val hm = k * v * v / (2.0 * G)

        return CalcOutput(
            results = listOf(result("hm", "Minor Head Loss", hm, "m", isPrimary = true)),
            steps = listOf(
                "Velocity head: v²/(2g) = ${Fmt.n(v, 3)}² / (2 × 9.80665) = ${Fmt.n(v * v / (2.0 * G), 5)} m",
                "Minor loss: h_m = K·v²/(2g) = ${Fmt.n(k, 3)} × ${Fmt.n(v * v / (2.0 * G), 5)} = ${Fmt.n(hm, 4)} m",
            ),
            warnings = emptyList(),
        )
    }
}
