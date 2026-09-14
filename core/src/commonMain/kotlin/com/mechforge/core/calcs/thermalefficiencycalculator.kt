package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Thermal efficiency of a cycle: η = W_net/Q_in. */
private val Def = CalculatorDefinition(
    id = "thermal-efficiency",
    name = "Thermal Efficiency",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "First-law thermal efficiency of any power cycle from net work and heat input.",
    formulaDisplay = "η = W_net / Q_in",
    reference = "First law of thermodynamics (energy balance).",
    notes = "Both energies must refer to the same cycle. Efficiency is a fraction; multiply by 100 for percent.",
    keywords = listOf("thermal efficiency", "cycle", "work", "heat", "first law"),
    inputs = listOf(
        InputSpec("w", "Net work output", "W_net", UnitFamily.ENERGY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kj"),
        InputSpec("q", "Heat input", "Q_in", UnitFamily.ENERGY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kj"),
    ),
)

object ThermalEfficiencyCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val w = value(inputs, "w")
        val q = value(inputs, "q")

        if (w > q) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("w", "Net work cannot exceed the heat input (first law)."))
            )
        }

        val eta = w / q

        return CalcOutput(
            results = listOf(
                result("eta", "Thermal Efficiency", eta * 100.0, "pct", isPrimary = true),
                result("ratio", "Efficiency (fraction)", eta, "dash"),
                result("rejected", "Heat Rejected", (q - w) / 1000.0, "kj"),
            ),
            steps = listOf(
                "η = W_net/Q_in = ${Fmt.n(w / 1000.0, 3)} kJ / ${Fmt.n(q / 1000.0, 3)} kJ = ${Fmt.n(eta, 4)} = ${Fmt.n(eta * 100.0, 2)} %",
                "Heat rejected: Q_out = Q_in − W_net = ${Fmt.n((q - w) / 1000.0, 3)} kJ",
            ),
            warnings = emptyList(),
        )
    }
}
