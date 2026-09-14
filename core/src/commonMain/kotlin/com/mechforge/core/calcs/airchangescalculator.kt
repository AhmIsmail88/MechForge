package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Air changes per hour: ACH = Q·3600/V_room. */
private val Def = CalculatorDefinition(
    id = "air-changes-hour",
    name = "Air Changes per Hour (ACH)",
    category = CalculatorCategory.HVAC,
    description = "Air changes per hour for a space from ventilation airflow and room volume.",
    formulaDisplay = "ACH = Q·3600 / V_room",
    reference = "Standard ventilation metric; ACH requirements come from codes — this tool only computes the value.",
    notes = "ACH is a screening metric. Compliance normally depends on occupancy, use and the applicable code or standard.",
    keywords = listOf("ach", "air changes", "ventilation", "fresh air", "room"),
    inputs = listOf(
        InputSpec("q", "Supply / ventilation airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("vroom", "Room volume", "V", UnitFamily.VOLUME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
    ),
)

object AirChangesCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val v = value(inputs, "vroom")

        val ach = q * 3600.0 / v

        return CalcOutput(
            results = listOf(
                result("ach", "Air Changes per Hour (1/h)", ach, "perh", isPrimary = true),
                result("time", "Air Change Time", 3600.0 / ach, "s"),
            ),
            steps = listOf(
                "Airflow: ${Fmt.n(q, 5)} m³/s = ${Fmt.n(q * 3600.0, 1)} m³/h",
                "ACH = Q·3600/V = ${Fmt.n(q * 3600.0, 1)} / ${Fmt.n(v, 2)} = ${Fmt.n(ach, 2)} 1/h",
                "One air change every ${Fmt.n(3600.0 / ach, 1)} s (${Fmt.n(60.0 / ach, 2)} min)",
            ),
            warnings = if (ach < 2.0) listOf("Below 2 ACH — verify against the applicable ventilation requirement for this space.") else emptyList(),
        )
    }
}
