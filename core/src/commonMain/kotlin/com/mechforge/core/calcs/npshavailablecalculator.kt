package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** NPSH available at the pump suction. */
private val Def = CalculatorDefinition(
    id = "npsh-available",
    name = "NPSH Available",
    category = CalculatorCategory.HYDRAULICS,
    description = "Net Positive Suction Head available at the pump inlet, from absolute pressure, vapour pressure, static lift and suction losses.",
    formulaDisplay = "NPSHa = (p_atm − p_v)/(ρ·g) + h_static − h_friction",
    reference = "Standard pump suction-head relation (e.g. Karassik, Pump Handbook; HI standards).",
    notes = "g = 9.80665 m/s². h_static may be negative (pump above the liquid surface). Compare NPSHa with the pump curve NPSHr — this tool does not know your pump.",
    keywords = listOf("npsh", "cavitation", "pump", "suction", "vapour pressure"),
    inputs = listOf(
        InputSpec("patm", "Absolute pressure at surface", "p_atm", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("pv", "Vapour pressure of liquid", "p_v", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "kpa"),
        InputSpec("rho", "Liquid density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
        InputSpec("hs", "Static suction head", "h_static", UnitFamily.LENGTH, defaultUnitId = "m"),
        InputSpec("hf", "Suction line losses", "h_friction", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = false, defaultUnitId = "m"),
    ),
)

object NpshAvailableCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val pAtm = optionalValue(inputs, "patm", 101325.0)
        val pV = optionalValue(inputs, "pv", 2339.0)
        val rho = optionalValue(inputs, "rho", 998.2)
        val hStatic = value(inputs, "hs")
        val hFriction = value(inputs, "hf")

        val pressureHead = (pAtm - pV) / (rho * G)
        val npsha = pressureHead + hStatic - hFriction

        val warnings = buildList {
            if (!has(inputs, "pv")) add("Vapour pressure not provided — assumed 2.339 kPa (water at 20 °C).")
            if (!has(inputs, "rho")) add("Density not provided — assumed 998.2 kg/m³ (water at 20 °C).")
            if (npsha < 3.0) add("NPSHa below 3 m — low margin. Verify against the pump NPSHr curve with an adequate safety margin.")
            if (npsha <= 0.0) add("NPSHa is not positive — the pump would cavitate at this duty.")
        }

        return CalcOutput(
            results = listOf(
                result("npsha", "NPSH Available", npsha, "m", isPrimary = true),
                result("phead", "Pressure Head Contribution", pressureHead, "m"),
            ),
            steps = listOf(
                "Pressure head: (p_atm − p_v)/(ρ·g) = (${Fmt.n(pAtm, 0)} − ${Fmt.n(pV, 0)}) / (${Fmt.n(rho, 1)} × 9.80665) = ${Fmt.n(pressureHead, 3)} m",
                "Static head: ${Fmt.n(hStatic, 3)} m   Suction losses: −${Fmt.n(hFriction, 3)} m",
                "NPSHa = ${Fmt.n(pressureHead, 3)} + ${Fmt.n(hStatic, 3)} − ${Fmt.n(hFriction, 3)} = ${Fmt.n(npsha, 3)} m",
            ),
            warnings = warnings,
        )
    }
}
