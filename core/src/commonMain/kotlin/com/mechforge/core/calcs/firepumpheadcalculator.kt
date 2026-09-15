package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Fire pump (booster) total head from pressures, static lift and losses. */
private val Def = CalculatorDefinition(
    id = "fire-pump-head",
    name = "Fire Pump Head (Booster)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Total head a fire pump must develop from the required discharge pressure, the available supply pressure, static elevation and friction losses.",
    formulaDisplay = "H = (P_req - P_avail)/(rho*g) + H_static + H_friction",
    reference = "Bernoulli/energy balance applied to a booster pump (NFPA 20 practice).",
    notes = "Pressures are ABSOLUTE or gauge, as long as both are on the same basis. This gives the pump duty point; the pump is then selected from its certified curve (NFPA 20).",
    keywords = listOf("fire pump", "booster", "head", "nfpa 20", "fire", "duty point"),
    inputs = listOf(
        InputSpec("preq", "Required discharge pressure", "P_req", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("pavail", "Available supply pressure", "P_avail", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "bar"),
        InputSpec("hstatic", "Static elevation gain", "H_static", UnitFamily.LENGTH, defaultUnitId = "m"),
        InputSpec("hf", "Friction and minor losses", "H_friction", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = false, defaultUnitId = "m"),
        InputSpec("rho", "Water density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
    ),
)

object FirePumpHeadCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val pReq = value(inputs, "preq")
        val pAvail = optionalValue(inputs, "pavail", 0.0)
        val hStatic = value(inputs, "hstatic")
        val hFriction = value(inputs, "hf")
        val rho = optionalValue(inputs, "rho", 998.2)

        val pressureHead = (pReq - pAvail) / (rho * G)
        val total = pressureHead + hStatic + hFriction

        return CalcOutput(
            results = listOf(
                result("h", "Total Pump Head", total, "m", isPrimary = true),
                result("phead", "Pressure Head Contribution", pressureHead, "m"),
                result("dp", "Pressure Difference", (pReq - pAvail) / 1e5, "bar"),
            ),
            steps = listOf(
                "Pressure difference: ${Fmt.n((pReq - pAvail) / 1e5, 3)} bar",
                "Pressure head: (P_req - P_avail)/(rho*g) = ${Fmt.n(pReq - pAvail, 0)} / (${Fmt.n(rho, 1)} x 9.80665) = ${Fmt.n(pressureHead, 3)} m",
                "Static elevation: ${Fmt.n(hStatic, 2)} m   Friction and minor losses: ${Fmt.n(hFriction, 2)} m",
                "Total head: H = ${Fmt.n(pressureHead, 3)} + ${Fmt.n(hStatic, 2)} + ${Fmt.n(hFriction, 2)} = ${Fmt.n(total, 2)} m",
            ),
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 998.2 kg/m3 (water at 20 C).")
                if (total <= 0.0) add("Total head is not positive - the supply pressure already exceeds the requirement.")
                add("Select the pump from its certified curve (NFPA 20 churn/rated/overload points).")
            },
        )
    }
}
