package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Carnot efficiency limit: η = 1 − T_c/T_h (absolute temperatures). */
private val Def = CalculatorDefinition(
    id = "carnot-efficiency",
    name = "Carnot Efficiency",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "Maximum theoretical thermal efficiency of a heat engine operating between two reservoirs.",
    formulaDisplay = "η_Carnot = 1 − T_c/T_h",
    reference = "Sadi Carnot (1824); second law of thermodynamics.",
    notes = "Temperatures are ABSOLUTE (K). No real cycle reaches this limit.",
    keywords = listOf("carnot", "efficiency", "thermal", "second law", "reversible"),
    inputs = listOf(
        InputSpec("th", "Hot reservoir temperature", "T_h", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
        InputSpec("tc", "Cold reservoir temperature", "T_c", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
    ),
)

object CarnotEfficiencyCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val th = value(inputs, "th")
        val tc = value(inputs, "tc")

        if (tc >= th) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("tc", "Cold reservoir temperature must be lower than the hot reservoir temperature."))
            )
        }

        val eta = 1.0 - tc / th

        return CalcOutput(
            results = listOf(
                result("eta", "Carnot Efficiency", eta * 100.0, "pct", isPrimary = true),
                result("ratio", "Efficiency (fraction)", eta, "dash"),
            ),
            steps = listOf(
                "Absolute temperatures: T_h = ${Fmt.n(th, 2)} K, T_c = ${Fmt.n(tc, 2)} K",
                "η = 1 − T_c/T_h = 1 − ${Fmt.n(tc, 2)}/${Fmt.n(th, 2)} = ${Fmt.n(eta, 4)} = ${Fmt.n(eta * 100.0, 2)} %",
            ),
            warnings = emptyList(),
            stepsAr = listOf(
                "الحرارات المطلقة: T_h = ${Fmt.n(th, 2)} K، T_c = ${Fmt.n(tc, 2)} K",
                "η = 1 − T_c/T_h = 1 − ${Fmt.n(tc, 2)}/${Fmt.n(th, 2)} = ${Fmt.n(eta, 4)} = ${Fmt.n(eta * 100.0, 2)} %",
            ),
        )
    }
}
