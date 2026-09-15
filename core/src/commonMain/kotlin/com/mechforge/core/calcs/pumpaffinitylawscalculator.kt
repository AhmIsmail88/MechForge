package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Pump affinity laws: speed change and impeller trim. */
private val Def = CalculatorDefinition(
    id = "pump-affinity-laws",
    name = "Pump Affinity Laws",
    category = CalculatorCategory.EQUIPMENT,
    description = "Predicted flow, head and power at a new pump speed, and for an impeller diameter change (trim).",
    formulaDisplay = "Q2 = Q1*(N2/N1) ;  H2 = H1*(N2/N1)^2 ;  P2 = P1*(N2/N1)^3",
    reference = "Pump affinity (similarity) laws; HI/ANSI pump standards practice.",
    notes = "Speed change: exact for a geometrically similar pump. Impeller trim: an approximation valid for trims up to about 10-15% - always verify on the actual curve. Efficiency is assumed unchanged.",
    keywords = listOf("affinity", "pump", "speed", "trim", "impeller", "equipment", "laws"),
    inputs = listOf(
        InputSpec("q1", "Flow at N1", "Q1", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("h1", "Head at N1", "H1", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("p1", "Power at N1", "P1", UnitFamily.POWER, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
        InputSpec("n1", "Original speed", "N1", UnitFamily.ROTATIONAL_SPEED, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec("n2", "New speed", "N2", UnitFamily.ROTATIONAL_SPEED, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec("d1", "Original impeller diameter (optional)", "D1", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("d2", "Trimmed impeller diameter (optional)", "D2", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object PumpAffinityLawsCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q1 = value(inputs, "q1")
        val h1 = value(inputs, "h1")
        val p1 = value(inputs, "p1")
        val n1 = value(inputs, "n1")
        val n2 = value(inputs, "n2")

        val r = n2 / n1
        val results = mutableListOf(
            result("q2", "Flow at N2", q1 * r * 3600.0, "m3h", isPrimary = true),
            result("h2", "Head at N2", h1 * r * r, "m", isPrimary = true),
            result("p2", "Power at N2", p1 / 1000.0 * r * r * r, "kw", isPrimary = true),
        )
        val steps = mutableListOf(
            "Speed ratio: N2/N1 = ${Fmt.n(n2, 1)} / ${Fmt.n(n1, 1)} = ${Fmt.n(r, 4)}",
            "Flow: Q2 = Q1 x r = ${Fmt.n(q1 * 3600.0, 2)} x ${Fmt.n(r, 4)} = ${Fmt.n(q1 * r * 3600.0, 2)} m3/h",
            "Head: H2 = H1 x r^2 = ${Fmt.n(h1, 3)} x ${Fmt.n(r * r, 4)} = ${Fmt.n(h1 * r * r, 3)} m",
            "Power: P2 = P1 x r^3 = ${Fmt.n(p1 / 1000.0, 3)} x ${Fmt.n(r * r * r, 4)} = ${Fmt.n(p1 / 1000.0 * r * r * r, 3)} kW",
        )

        if (has(inputs, "d1") && has(inputs, "d2")) {
            val d1 = value(inputs, "d1")
            val d2 = value(inputs, "d2")
            val rd = d2 / d1
            results += result("q2t", "Flow at D2 (trim)", q1 * rd * 3600.0, "m3h")
            results += result("h2t", "Head at D2 (trim)", h1 * rd * rd, "m")
            results += result("p2t", "Power at D2 (trim)", p1 / 1000.0 * rd * rd * rd, "kw")
            steps += "Trim ratio: D2/D1 = ${Fmt.n(rd, 4)}"
            steps += "Trim case: Q = ${Fmt.n(q1 * rd * 3600.0, 2)} m3/h, H = ${Fmt.n(h1 * rd * rd, 3)} m, P = ${Fmt.n(p1 / 1000.0 * rd * rd * rd, 3)} kW"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (r <= 0.0) add("Speed ratio is not positive - check the entered speeds.")
                if (r < 0.5 || r > 2.0) add("Speed ratio outside 0.5-2.0: the affinity laws become unreliable and the pump may be unstable.")
                if (has(inputs, "d1") && has(inputs, "d2")) {
                    val rd = value(inputs, "d2") / value(inputs, "d1")
                    if (rd < 0.85 || rd > 1.05) add("Trim beyond about 15%: affinity laws are approximate - use the manufacturer's trimmed curve.")
                }
            },
        )
    }
}
