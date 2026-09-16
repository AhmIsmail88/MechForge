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

/** Heat exchanger duty from the fluid side (m_dot*cp*dT) or the area side (U*A*LMTD). */
private val Def = CalculatorDefinition(
    id = "heat-exchanger-duty",
    name = "Heat Exchanger Duty",
    category = CalculatorCategory.EQUIPMENT,
    description = "Heat duty of a heat exchanger from the fluid side, cross-checked with U x A x LMTD when the area and terminal temperatures are given.",
    formulaDisplay = "Q = m_dot*cp*(T_out - T_in) ;  Q = U*A*LMTD",
    reference = "Standard heat-exchanger relations (e.g. Incropera & DeWitt, Fundamentals of Heat and Mass Transfer).",
    notes = "cp defaults to 4186 J/(kg.K) (water). The area-side check needs U, A and the four terminal temperatures; LMTD assumes constant U and no phase change.",
    keywords = listOf("heat exchanger", "duty", "lmtd", "u value", "thermal", "equipment"),
    inputs = listOf(
        InputSpec("m", "Mass flow", "m_dot", UnitFamily.MASS_FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgs"),
        InputSpec("cp", "Specific heat", "cp", UnitFamily.SPECIFIC_HEAT, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kjkgk", assumedWhenOmitted = "Specific heat assumed as 4186 J/(kg.K) (water) - use the value for the fluid actually handled."),
        InputSpec("tin", "Inlet temperature", "T_in", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("tout", "Outlet temperature", "T_out", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("u", "Overall coefficient U (optional)", "U", UnitFamily.HEAT_TRANSFER_COEFF, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "wm2k"),
        InputSpec("a", "Heat transfer area A (optional)", "A", UnitFamily.AREA, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m2"),
        InputSpec("thin", "Hot inlet (optional)", "T_h,in", UnitFamily.TEMPERATURE, required = false, defaultUnitId = "c"),
        InputSpec("thout", "Hot outlet (optional)", "T_h,out", UnitFamily.TEMPERATURE, required = false, defaultUnitId = "c"),
        InputSpec("tcin", "Cold inlet (optional)", "T_c,in", UnitFamily.TEMPERATURE, required = false, defaultUnitId = "c"),
        InputSpec("tcout", "Cold outlet (optional)", "T_c,out", UnitFamily.TEMPERATURE, required = false, defaultUnitId = "c"),
        InputSpec("arr", "Arrangement (1 = counter, 0 = parallel)", "arr", UnitFamily.DIMENSIONLESS, required = false, defaultUnitId = "dash"),
    ),
)

object HeatExchangerDutyCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val m = value(inputs, "m")
        val cp = optionalValue(inputs, "cp", 4186.0)
        val dT = value(inputs, "tout") - value(inputs, "tin")
        val qFluid = m * cp * dT

        val results = mutableListOf(
            result("q", "Heat Duty (fluid side)", qFluid / 1000.0, "kw", isPrimary = true),
            result("qBtuh", "Heat Duty (imperial)", qFluid / 1000.0 * 3412.142, "btuh"),
            result("dt", "Temperature Difference", dT, "delk"),
        )
        val steps = mutableListOf(
            "m_dot = ${Fmt.n(m, 4)} kg/s   cp = ${Fmt.n(cp / 1000.0, 4)} kJ/(kg.K)   dT = ${Fmt.n(dT, 2)} K",
            "Q = m_dot*cp*dT = ${Fmt.n(m, 4)} x ${Fmt.n(cp / 1000.0, 4)} x ${Fmt.n(dT, 2)} = ${Fmt.n(qFluid / 1000.0, 3)} kW",
        )

        val hasAreaSide = has(inputs, "u") && has(inputs, "a") &&
            has(inputs, "thin") && has(inputs, "thout") && has(inputs, "tcin") && has(inputs, "tcout")
        if (hasAreaSide) {
            val u = value(inputs, "u")
            val a = value(inputs, "a")
            val thin = value(inputs, "thin")
            val thout = value(inputs, "thout")
            val tcin = value(inputs, "tcin")
            val tcout = value(inputs, "tcout")
            val counter = optionalValue(inputs, "arr", 1.0) >= 0.5
            val dt1 = if (counter) thin - tcout else thin - tcin
            val dt2 = if (counter) thout - tcin else thout - tcout
            if (dt1 > 0.0 && dt2 > 0.0) {
                val lmtd = if (abs(dt1 - dt2) < 1e-9) dt1 else (dt1 - dt2) / ln(dt1 / dt2)
                val qArea = u * a * lmtd
                results += result("qArea", "Heat Duty (area side)", qArea / 1000.0, "kw", isPrimary = true)
                results += result("lmtd", "LMTD", lmtd, "delk")
                steps += "LMTD = ${Fmt.n(lmtd, 3)} K (${if (counter) "counter-current" else "parallel-flow"})"
                steps += "Area side: Q = U*A*LMTD = ${Fmt.n(u, 1)} x ${Fmt.n(a, 4)} x ${Fmt.n(lmtd, 3)} = ${Fmt.n(qArea / 1000.0, 3)} kW"
                steps += "Cross-check: fluid side ${Fmt.n(qFluid / 1000.0, 3)} kW vs area side ${Fmt.n(qArea / 1000.0, 3)} kW (difference ${Fmt.n(abs(qFluid - qArea) / maxOf(abs(qFluid), 1e-9) * 100.0, 1)}%)"
            } else {
                steps += "Area side not computed: the terminal temperature differences are not positive for the selected arrangement."
            }
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "cp")) add("Specific heat not provided - assumed 4186 J/(kg.K) (water).")
                if (!hasAreaSide) add("Area side (U x A x LMTD) skipped - provide U, A and the four terminal temperatures for the cross-check.")
                if (dT < 0.0) add("Outlet is colder than the inlet - the duty is negative (heat removed from this stream).")
            },
        )
    }
}
