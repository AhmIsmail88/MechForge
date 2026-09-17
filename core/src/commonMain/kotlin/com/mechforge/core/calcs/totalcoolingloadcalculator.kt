package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/**
 * Total cooling load = sensible + latent for an air stream.
 * Q_s = ρ·V̇·c_p·ΔT ; Q_l = ρ·V̇·h_fg·Δw ; Q_t = Q_s + Q_l
 */

private val Def = CalculatorDefinition(
    id = "total-cooling-load",
    name = "Total Cooling Load (Air-Side)",
    category = CalculatorCategory.HVAC,
    description = "Sensible, latent and total cooling load for an air stream from temperature and humidity difference.",
    formulaDisplay = "Q_t = ρ·V̇·(c_p·ΔT + h_fg·Δw)",
    reference = "Standard psychrometric load relations (formula form; ASHRAE Handbook—Fundamentals).",
    notes = "h_fg = 2501 kJ/kg. Humidity ratio w in kg/kg dry air (optional — omit for sensible-only).",
    keywords = listOf("cooling", "load", "sensible", "latent", "coil", "hvac"),
    inputs = listOf(
        InputSpec("q", "Airflow", "V̇", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("tin", "Entering dry-bulb", "T_in", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("tout", "Leaving dry-bulb", "T_out", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("win", "Entering humidity ratio", "w_in", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("wout", "Leaving humidity ratio", "w_out", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("rho", "Air density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
    ),
)

object TotalCoolingLoadCalculator : Calculator(Def) {

    private const val CP_AIR = 1.005 // kJ/(kg·K)
    private const val H_FG = 2501.0 // kJ/kg at 0°C, standard approximation

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val tIn = value(inputs, "tin")
        val tOut = value(inputs, "tout")
        val hasHumidity = has(inputs, "win") && has(inputs, "wout")
        val rho = optionalValue(inputs, "rho", 1.2)

        val massFlow = rho * q
        val deltaT = tOut - tIn
        val qs = massFlow * CP_AIR * deltaT

        var ql = 0.0
        var deltaW = 0.0
        if (hasHumidity) {
            val wIn = value(inputs, "win")
            val wOut = value(inputs, "wout")
            deltaW = wOut - wIn
            ql = massFlow * H_FG * deltaW
        }

        val total = qs + ql

        val warnings = buildList {
            if (!hasHumidity) {
                add("Humidity ratios not provided — latent load computed as 0. Enter humidity ratio (kg/kg dry air) for a full load split.")
            }
            if (deltaT > 0) add("ΔT > 0 — the air GAINS sensible heat; for a cooling coil temperatures normally drop.")
        }

        return CalcOutput(
            results = buildList {
                add(result("qs", "Sensible Load", qs, "kw"))
                add(result("ql", "Latent Load", ql, "kw"))
                add(result("qt", "Total Cooling Load", total, "kw", isPrimary = true))
            },
            steps = buildList {
                add("Mass flow: ṁ = ρ·V̇ = ${Fmt.n(rho, 3)} × ${Fmt.n(q, 4)} = ${Fmt.n(massFlow, 4)} kg/s")
                add("Sensible: Q_s = ṁ·c_p·ΔT = ${Fmt.n(massFlow, 4)} × 1.005 × ${Fmt.n(deltaT, 2)} = ${Fmt.n(qs, 3)} kW")
                if (hasHumidity) {
                    add("Latent: Q_l = ṁ·h_fg·Δw = ${Fmt.n(massFlow, 4)} × 2501 × ${Fmt.n(deltaW, 5)} = ${Fmt.n(ql, 3)} kW")
                } else {
                    add("Latent: not computed (humidity ratios missing) → 0 kW")
                }
                add("Total: Q_t = Q_s + Q_l = ${Fmt.n(total, 3)} kW")
            },
            stepsAr = buildList {
                add("معدل الكتلة: ṁ = ρ·V̇ = ${Fmt.n(rho, 3)} × ${Fmt.n(q, 4)} = ${Fmt.n(massFlow, 4)} kg/s")
                add("المحسوس: Q_s = ṁ·c_p·ΔT = ${Fmt.n(massFlow, 4)} × 1.005 × ${Fmt.n(deltaT, 2)} = ${Fmt.n(qs, 3)} kW")
                if (hasHumidity) {
                    add("الكامن: Q_l = ṁ·h_fg·Δw = ${Fmt.n(massFlow, 4)} × 2501 × ${Fmt.n(deltaW, 5)} = ${Fmt.n(ql, 3)} kW")
                } else {
                    add("الكامن: لم يُحسب (نسب الرطوبة ناقصة) ← 0 kW")
                }
                add("الإجمالي: Q_t = Q_s + Q_l = ${Fmt.n(total, 3)} kW")
            },
            warnings = warnings,
            warningsAr = buildList {
                if (!hasHumidity) {
                    add("لم تُدخل نسب الرطوبة - الحمل الكامن محسوب كصفر. أدخل نسبة الرطوبة (kg/kg هواء جاف) لتقسيم كامل.")
                }
                if (deltaT > 0) {
                    add("ΔT > 0 - الهواء يكتسب حرارة محسوسة؛ وفي ملف التبريد عادة تنخفض الحرارة.")
                }
            },
        )
    }

}
