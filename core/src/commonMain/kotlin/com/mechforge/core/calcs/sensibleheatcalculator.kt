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
 * Sensible heat for an air stream.
 * Metric:  Q_s = ρ · V̇ · c_p · ΔT          (kW, with V̇ in m³/s)
 * Imperial: Q_s = 1.08 · CFM · ΔT(°F)       (BTU/h, standard air)
 */

private val Def = CalculatorDefinition(
    id = "sensible-heat",
    name = "Sensible Heat (Air)",
    category = CalculatorCategory.HVAC,
    description = "Sensible heat added to or removed from an air stream, in SI and imperial forms.",
    formulaDisplay = "Q_s = ρ·V̇·c_p·ΔT   (imperial: Q_s = 1.08·CFM·ΔT)",
    reference = "Standard air-side HVAC relations (formula form; ASHRAE Handbook—Fundamentals).",
    notes = "c_p = 1.005 kJ/(kg·K). Imperial 1.08-form assumes standard air density; shown as equivalent of the computed kW.",
    keywords = listOf("sensible", "heat", "cooling", "heating", "hvac", "air"),
    inputs = listOf(
        InputSpec("q", "Airflow", "V̇", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("tin", "Entering dry-bulb", "T_in", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("tout", "Leaving dry-bulb", "T_out", UnitFamily.TEMPERATURE, defaultUnitId = "c"),
        InputSpec("rho", "Air density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
    ),
)

object SensibleHeatCalculator : Calculator(Def) {

    private const val CP_AIR = 1.005 // kJ/(kg·K)
    private const val BTUH_PER_KW = 3412.142

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val tIn = value(inputs, "tin") // K
        val tOut = value(inputs, "tout") // K
        val rho = optionalValue(inputs, "rho", 1.2) // kg/m³

        val deltaT = tOut - tIn
        val massFlow = rho * q // kg/s
        val qsKw = massFlow * CP_AIR * deltaT
        val qsBtuh = qsKw * BTUH_PER_KW

        val warnings = buildList {
            if (abs(deltaT) < 1e-9) add("Supply and return temperatures are equal — load is zero.")
            if (deltaT > 0) add("ΔT > 0 — this is a heating duty (air gains heat).")
            if (!has(inputs, "rho")) add("Air density not provided — assumed 1.2 kg/m³ (standard air ~20°C).")
        }

        return CalcOutput(
            results = listOf(
                result("qs", "Sensible Heat", qsKw, "kw", isPrimary = true),
                result("qsBtuh", "Sensible Heat (imperial)", qsBtuh, "btuh", isPrimary = false),
            ),
            steps = listOf(
                "Mass flow: ṁ = ρ·V̇ = ${Fmt.n(rho, 3)} × ${Fmt.n(q, 4)} = ${Fmt.n(massFlow, 4)} kg/s",
                "ΔT = ${Fmt.n(tOut, 2)} − ${Fmt.n(tIn, 2)} = ${Fmt.n(deltaT, 2)} K",
                "Q_s = ṁ·c_p·ΔT = ${Fmt.n(massFlow, 4)} × 1.005 × ${Fmt.n(deltaT, 2)} = ${Fmt.n(qsKw, 3)} kW",
                "Imperial check (standard air form): 1.08 × CFM × ΔT(°F) equivalent = ${Fmt.n(qsBtuh, 0)} BTU/h",
            ),
            warnings = warnings,
        )
    }

    private fun abs(x: Double) = if (x < 0) -x else x

}
