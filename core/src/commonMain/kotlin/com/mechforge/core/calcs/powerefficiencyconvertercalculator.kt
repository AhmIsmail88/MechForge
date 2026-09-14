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
 * Power / efficiency relations for cooling equipment:
 * kW ↔ TR (1 TR = 3.517 kW), COP = Q_th / P_elec, EER = 3.412·COP, kW/TR = P_elec / TR.
 */

private val Def = CalculatorDefinition(
    id = "power-efficiency-converter",
    name = "kW ↔ TR / COP / EER",
    category = CalculatorCategory.HVAC,
    description = "Converts cooling power between kW and TR; computes COP, EER and kW/TR when electrical input is given.",
    formulaDisplay = "1 TR = 3.517 kW ; COP = Q_th/P_elec ; EER = 3.412·COP",
    reference = "Standard definitions: 1 TR = 12,000 BTU/h = 3.5168528 kW.",
    notes = "Enter electrical input power to get COP/EER/kW/TR. All three must refer to the same duty point.",
    keywords = listOf("tr", "ton of refrigeration", "cop", "eer", "kw", "chiller", "efficiency"),
    inputs = listOf(
        InputSpec("p", "Cooling / thermal power", "Q_th", UnitFamily.POWER, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
        InputSpec("pelec", "Electrical input power", "P_elec", UnitFamily.POWER, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
    ),
)

object PowerEfficiencyConverterCalculator : Calculator(Def) {

    private const val KW_PER_TR = 3.5168528
    private const val BTUH_PER_W = 3.412141633

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p") // W (cooling capacity or thermal power)
        val hasElec = has(inputs, "pelec")

        val pKw = p / 1000.0
        val tr = p / KW_PER_TR / 1000.0

        val results = mutableListOf(
            result("kw", "Power", pKw, "kw", isPrimary = true),
            result("tr", "Tons of Refrigeration", tr, "tr", isPrimary = true),
        )
        val steps = mutableListOf(
            "kW → TR: divide by ${KW_PER_TR} kW/TR → ${Fmt.n(pKw, 3)} kW = ${Fmt.n(tr, 3)} TR",
        )

        if (hasElec) {
            val pelec = value(inputs, "pelec") // W electrical
            if (pelec <= 0.0) {
                throw com.mechforge.core.engine.ValidationException(
                    listOf(com.mechforge.core.engine.InputError("pelec", "Electrical power must be positive."))
                )
            }
            val cop = p / pelec
            val eer = cop * BTUH_PER_W
            val kwPerTr = pelec / 1000.0 / tr
            results += listOf(
                result("cop", "COP", cop, "dash", isPrimary = false),
                result("eer", "EER", eer, "dash", isPrimary = false),
                result("kwtr", "kW per TR (electrical)", kwPerTr, "dash", isPrimary = false),
            )
            steps += listOf(
                "COP = Q_th / P_elec = ${Fmt.n(pKw, 3)} / ${Fmt.n(pelec / 1000.0, 3)} = ${Fmt.n(cop, 3)}",
                "EER = COP × 3.412 = ${Fmt.n(eer, 2)} BTU/h per W",
                "kW/TR = P_elec / TR = ${Fmt.n(pelec / 1000.0, 3)} / ${Fmt.n(tr, 3)} = ${Fmt.n(kwPerTr, 3)}",
            )
        }

        return CalcOutput(results = results, steps = steps, warnings = emptyList())
    }

}
