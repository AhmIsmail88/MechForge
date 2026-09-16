package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Airflow unit converter expressed as a calculator: one input, all common units out. */

private val Def = CalculatorDefinition(
    id = "airflow-converter",
    name = "Airflow Unit Converter",
    category = CalculatorCategory.HVAC,
    description = "Converts an airflow value between CFM, m³/h, L/s and L/min.",
    formulaDisplay = "1 CFM = 1.699011 m³/h = 0.471947 L/s",
    reference = "Exact unit definitions: 1 ft³ = 0.028316846592 m³; 1 US gal = 3.785411784 L.",
    notes = "Uses the global unit system, so any flow unit can be entered.",
    keywords = listOf("airflow", "converter", "cfm", "m3/h", "l/s", "conversion"),
    inputs = listOf(
        InputSpec("q", "Airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "cfm"),
    ),
)

object AirflowConverterCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val entered = inputs.getValue("q").displayUnitId

        return CalcOutput(
            results = listOf(
                result("m3h", "Cubic metres per hour", q * 3600.0, "m3h", isPrimary = true),
                result("cfm", "Cubic feet per minute", q / 4.719474432e-4, "cfm", isPrimary = true),
                result("ls", "Litres per second", q * 1e3, "ls", isPrimary = true),
                result("lmin", "Litres per minute", q * 1e3 * 60.0, "lmin", isPrimary = false),
            ),
            steps = listOf("Converted from ${UnitsSymbol(entered)} via exact factors."),
            stepsAr = listOf("تم التحويل من ${UnitsSymbol(entered)} باستخدام معاملات دقيقة."),
            warnings = emptyList(),
        )
    }

    private fun UnitsSymbol(id: String): String = when (id) {
        "m3h" -> "m³/h"; "cfm" -> "CFM"; "ls" -> "L/s"; "lmin" -> "L/min"; "gpm" -> "GPM"; else -> id
    }

}
