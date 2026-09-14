package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.abs
import kotlin.math.ln

/** Log mean temperature difference for heat exchangers. */
private val Def = CalculatorDefinition(
    id = "lmtd",
    name = "LMTD (Heat Exchanger)",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "Log mean temperature difference for counter-current or parallel-flow heat exchangers, with duty when U and A are given.",
    formulaDisplay = "LMTD = (ΔT₁ − ΔT₂)/ln(ΔT₁/ΔT₂) ;  Q = U·A·LMTD",
    reference = "Standard heat-exchanger relations (e.g. Incropera & DeWitt, Fundamentals of Heat and Mass Transfer).",
    notes = "Arrangement is selected by the user (counter-current inlets: Th_in/Tc_out, parallel: Th_in/Tc_in). LMTD assumes constant U and no phase change.",
    keywords = listOf("lmtd", "heat exchanger", "duty", "u value", "temperature difference"),
    inputs = listOf(
        InputSpec("thin", "Hot inlet temperature", "T_h,in", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("thout", "Hot outlet temperature", "T_h,out", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("tcin", "Cold inlet temperature", "T_c,in", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("tcout", "Cold outlet temperature", "T_c,out", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("arr", "Arrangement (1 = counter-current, 0 = parallel)", "arr", UnitFamily.DIMENSIONLESS, required = false, defaultUnitId = "dash"),
        InputSpec("u", "Overall coefficient U (optional)", "U", UnitFamily.HEAT_TRANSFER_COEFF, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "wm2k"),
        InputSpec("a", "Heat transfer area A (optional)", "A", UnitFamily.AREA, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m2"),
    ),
)

object LmtdCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val thIn = value(inputs, "thin")
        val thOut = value(inputs, "thout")
        val tcIn = value(inputs, "tcin")
        val tcOut = value(inputs, "tcout")
        val counter = optionalValue(inputs, "arr", 1.0) >= 0.5

        val dt1 = if (counter) thIn - tcOut else thIn - tcIn
        val dt2 = if (counter) thOut - tcIn else thOut - tcOut

        if (dt1 <= 0.0 || dt2 <= 0.0) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "thin",
                        "Terminal temperature differences must be positive (check the arrangement and the four temperatures)."
                    )
                )
            )
        }

        val lmtd = if (abs(dt1 - dt2) < 1e-9) dt1 else (dt1 - dt2) / ln(dt1 / dt2)

        val results = mutableListOf(
            result("lmtd", "LMTD", lmtd, "delk", isPrimary = true),
            result("dt1", "Terminal Difference 1", dt1, "delk"),
            result("dt2", "Terminal Difference 2", dt2, "delk"),
        )
        val steps = mutableListOf(
            "Arrangement: ${if (counter) "counter-current" else "parallel-flow"}",
            "Terminal differences: ΔT₁ = ${Fmt.n(dt1, 2)} K, ΔT₂ = ${Fmt.n(dt2, 2)} K",
            "LMTD = (ΔT₁ − ΔT₂)/ln(ΔT₁/ΔT₂) = (${Fmt.n(dt1, 2)} − ${Fmt.n(dt2, 2)})/ln(${Fmt.n(dt1, 2)}/${Fmt.n(dt2, 2)}) = ${Fmt.n(lmtd, 3)} K",
        )

        if (has(inputs, "u") && has(inputs, "a")) {
            val u = value(inputs, "u")
            val a = value(inputs, "a")
            val duty = u * a * lmtd
            results += result("q", "Heat Duty", duty / 1000.0, "kw", isPrimary = true)
            steps += "Duty: Q = U·A·LMTD = ${Fmt.n(u, 1)} × ${Fmt.n(a, 4)} × ${Fmt.n(lmtd, 3)} = ${Fmt.n(duty / 1000.0, 3)} kW"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = if (!has(inputs, "arr")) {
                listOf("Arrangement not provided — assumed counter-current (default 1).")
            } else {
                emptyList()
            },
        )
    }
}
