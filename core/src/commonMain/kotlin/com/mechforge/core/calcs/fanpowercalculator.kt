package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Fan shaft power and motor power: P = Q·Δp/η. */
private val Def = CalculatorDefinition(
    id = "fan-power",
    name = "Fan Power",
    category = CalculatorCategory.HVAC,
    description = "Air power, shaft power and motor power for a fan from airflow, total pressure and efficiencies.",
    formulaDisplay = "P_air = Q·Δp ;  P_shaft = P_air/η_fan ;  P_motor = P_shaft/η_drive",
    reference = "Standard fan power relations (air power = flow × total pressure).",
    notes = "Δp is the fan TOTAL pressure (Pa). η_fan is the fan efficiency at the duty point; η_drive covers belt/direct-drive losses and any margin.",
    keywords = listOf("fan", "power", "air", "hvac", "motor", "static pressure"),
    inputs = listOf(
        InputSpec("q", "Airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("dp", "Fan total pressure", "Δp", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "pa"),
        InputSpec("etaf", "Fan efficiency", "η_fan", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct"),
        InputSpec("etad", "Drive / transmission efficiency", "η_drive", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct"),
    ),
)

object FanPowerCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val dp = value(inputs, "dp")
        val etaFan = value(inputs, "etaf")
        val etaDrive = optionalValue(inputs, "etad", 1.0)

        val pAir = q * dp
        val pShaft = pAir / etaFan
        val pMotor = pShaft / etaDrive

        val warnings = buildList {
            if (!has(inputs, "etad")) add("Drive efficiency not provided — assumed 1.0 (direct drive, no margin).")
            if (etaFan > 0.8) add("Fan efficiency above 80% is optimistic for small fans; verify at the duty point on the fan curve.")
        }

        return CalcOutput(
            results = listOf(
                result("pair", "Air Power", pAir / 1000.0, "kw"),
                result("pshaft", "Shaft Power", pShaft / 1000.0, "kw", isPrimary = true),
                result("pmotor", "Motor Power", pMotor / 1000.0, "kw", isPrimary = true),
            ),
            steps = listOf(
                "Air power: P_air = Q·Δp = ${Fmt.n(q, 5)} × ${Fmt.n(dp, 1)} = ${Fmt.n(pAir, 2)} W",
                "Shaft power: P_shaft = P_air/η_fan = ${Fmt.n(pAir, 2)} / ${Fmt.n(etaFan, 4)} = ${Fmt.n(pShaft, 2)} W = ${Fmt.n(pShaft / 1000.0, 3)} kW",
                "Motor power: P_motor = P_shaft/η_drive = ${Fmt.n(pMotor / 1000.0, 3)} kW",
            ),
            warnings = warnings,
        )
    }
}
