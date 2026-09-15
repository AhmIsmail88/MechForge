package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Fan laws with the air-density correction. */
private val Def = CalculatorDefinition(
    id = "fan-laws",
    name = "Fan Laws",
    category = CalculatorCategory.EQUIPMENT,
    description = "Predicted airflow, total pressure and shaft power at a new fan speed, including the air-density correction.",
    formulaDisplay = "Q2 = Q1*(N2/N1) ;  dP2 = dP1*(N2/N1)^2*(rho2/rho1) ;  P2 = P1*(N2/N1)^3*(rho2/rho1)",
    reference = "Fan laws (AMCA/ASHRAE practice): flow proportional to speed, pressure to speed squared and density, power to speed cubed and density.",
    notes = "Density correction: the flow is independent of density, but pressure and power scale with it. The laws apply to the same fan and system (no damper or blade-angle change).",
    keywords = listOf("fan", "laws", "speed", "density", "equipment", "airflow", "amca"),
    inputs = listOf(
        InputSpec("q1", "Airflow at N1", "Q1", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("dp1", "Total pressure at N1", "dP1", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "pa"),
        InputSpec("p1", "Shaft power at N1", "P1", UnitFamily.POWER, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
        InputSpec("n1", "Original speed", "N1", UnitFamily.ROTATIONAL_SPEED, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec("n2", "New speed", "N2", UnitFamily.ROTATIONAL_SPEED, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec("rho1", "Air density at N1 (optional)", "rho1", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
        InputSpec("rho2", "Air density at N2 (optional)", "rho2", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
    ),
)

object FanLawsCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q1 = value(inputs, "q1")
        val dp1 = value(inputs, "dp1")
        val p1 = value(inputs, "p1")
        val n1 = value(inputs, "n1")
        val n2 = value(inputs, "n2")
        val rho1 = optionalValue(inputs, "rho1", 1.2)
        val rho2 = optionalValue(inputs, "rho2", 1.2)

        val r = n2 / n1
        val densityRatio = rho2 / rho1
        val dp2 = dp1 * r * r * densityRatio
        val p2 = p1 / 1000.0 * r * r * r * densityRatio

        return CalcOutput(
            results = listOf(
                result("q2", "Airflow at N2", q1 * r * 3600.0, "m3h", isPrimary = true),
                result("dp2", "Total Pressure at N2", dp2, "pa", isPrimary = true),
                result("p2", "Shaft Power at N2", p2, "kw", isPrimary = true),
                result("rp", "Speed Ratio", r, "dash"),
                result("rd", "Density Ratio", densityRatio, "dash"),
            ),
            steps = listOf(
                "Speed ratio: N2/N1 = ${Fmt.n(n2, 1)} / ${Fmt.n(n1, 1)} = ${Fmt.n(r, 4)}",
                "Density ratio: rho2/rho1 = ${Fmt.n(rho2, 4)} / ${Fmt.n(rho1, 4)} = ${Fmt.n(densityRatio, 4)}",
                "Flow: Q2 = Q1 x r = ${Fmt.n(q1 * r * 3600.0, 1)} m3/h (density has no effect)",
                "Pressure: dP2 = dP1 x r^2 x (rho2/rho1) = ${Fmt.n(dp1, 1)} x ${Fmt.n(r * r, 4)} x ${Fmt.n(densityRatio, 4)} = ${Fmt.n(dp2, 1)} Pa",
                "Power: P2 = P1 x r^3 x (rho2/rho1) = ${Fmt.n(p1 / 1000.0, 3)} x ${Fmt.n(r * r * r, 4)} x ${Fmt.n(densityRatio, 4)} = ${Fmt.n(p2, 3)} kW",
            ),
            warnings = buildList {
                if (!has(inputs, "rho1") || !has(inputs, "rho2")) add("Densities not provided - assumed 1.2 kg/m3 for both (no density correction).")
                if (r < 0.5 || r > 1.5) add("Speed ratio far from 1: the fan laws assume the same system curve and no damper change - verify on the fan curve.")
                if (p2 > p1 / 1000.0 * 1.5) add("Power rises steeply with speed (N^3) - check the motor rating at the new duty.")
            },
        )
    }
}
