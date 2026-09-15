package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Fire pump power: hydraulic power, shaft power and a standard IEC driver rating. */
private val Def = CalculatorDefinition(
    id = "fire-pump-power",
    name = "Fire Pump Power",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Hydraulic and shaft power for a fire pump duty point, with the next standard IEC motor rating.",
    formulaDisplay = "P_h = rho*g*Q*H ;  P_shaft = P_h/eta",
    reference = "Pump power relations; IEC 60034 standard motor ratings (NFPA 20 governs the actual driver selection).",
    notes = "eta is the pump efficiency at the duty point (from the certified curve). NFPA 20 requires the driver to carry the pump at its overload point, so the selected rating is normally above the shaft power - verify against the standard.",
    keywords = listOf("fire pump", "power", "motor", "diesel", "fire", "nfpa 20"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "lmin"),
        InputSpec("h", "Total head", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("eta", "Pump efficiency", "eta", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct"),
        InputSpec("rho", "Water density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
    ),
)

object FirePumpPowerCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val h = value(inputs, "h")
        val eta = value(inputs, "eta")
        val rho = optionalValue(inputs, "rho", 998.2)

        val hydraulicKw = rho * G * q * h / 1000.0
        val shaftKw = hydraulicKw / eta
        val motor = IEC_MOTOR_RATINGS_KW.firstOrNull { it >= shaftKw }

        return CalcOutput(
            results = listOf(
                result("hydraulic", "Hydraulic Power", hydraulicKw, "kw"),
                result("shaft", "Shaft Power", shaftKw, "kw", isPrimary = true),
                result(
                    "motor", "Recommended Driver Rating",
                    motor ?: 200.0, "kw", isRecommended = true,
                ),
            ),
            steps = listOf(
                "Flow: ${Fmt.n(q * 60000.0, 1)} L/min = ${Fmt.n(q, 5)} m3/s",
                "Hydraulic power: P_h = rho*g*Q*H = ${Fmt.n(rho, 1)} x 9.80665 x ${Fmt.n(q, 5)} x ${Fmt.n(h, 2)} = ${Fmt.n(hydraulicKw, 2)} kW",
                "Shaft power: P = P_h/eta = ${Fmt.n(hydraulicKw, 2)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw, 2)} kW",
                "Next standard IEC rating: ${motor?.let { Fmt.n(it, 1) } ?: "> 200"} kW",
            ),
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 998.2 kg/m3 (water at 20 C).")
                if (motor == null) add("Shaft power exceeds 200 kW - select a larger or custom driver.")
                add("NFPA 20 requires the driver to be rated for the pump overload point - confirm before ordering.")
            },
        )
    }
}
