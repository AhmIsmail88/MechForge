package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ResultValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.units.Units
import com.mechforge.core.util.Fmt

/**
 * Family unit converters (README v2 §16 / §7.2 "Unit Conversion").
 * Each converter takes one value in any unit of its family and reports the value
 * in every other unit of the same family.
 */
private fun converterDefinition(id: String, name: String, family: UnitFamily, defaultUnitId: String) =
    CalculatorDefinition(
        id = id,
        name = name,
        category = CalculatorCategory.UNIT_CONVERSION,
        description = "Converts a ${family.displayName.lowercase()} value into every supported unit of the same family.",
        formulaDisplay = "value × (factor to SI) ÷ (factor from SI)",
        reference = "Exact unit definitions (ISO 80000 / NIST); see the unit registry for factors.",
        notes = "Uses the shared unit registry, so results are identical to the global converter screen.",
        keywords = listOf("convert", "converter", "unit", family.displayName.lowercase()),
        inputs = listOf(
            InputSpec("v", "Value", "x", family, defaultUnitId = defaultUnitId),
        ),
    )

/** Shared implementation for the family converters. */
abstract class FamilyConverter(
    def: CalculatorDefinition,
    private val family: UnitFamily,
    private val primaryUnitIds: Set<String>,
) : Calculator(def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val base = value(inputs, "v")
        val enteredUnit = Units.byId(inputs.getValue("v").displayUnitId)

        val results: List<ResultValue> = Units.byFamily(family).map { unit ->
            result(
                id = unit.id,
                label = unit.symbol,
                value = unit.fromBase(base),
                unitId = unit.id,
                isPrimary = unit.id in primaryUnitIds,
            )
        }

        return CalcOutput(
            results = results,
            steps = listOf(
                "Entered: ${Fmt.n(enteredUnit.fromBase(base), 6)} ${enteredUnit.symbol}",
                "Converted to SI base unit and back into every ${family.displayName} unit.",
            ),
            stepsAr = listOf(
                "المُدخل: ${Fmt.n(enteredUnit.fromBase(base), 6)} ${enteredUnit.symbol}",
                "تم التحويل إلى وحدة النظام الأساسية ثم إلى كل وحدات العائلة.",
            ),
            warnings = emptyList(),
            warningsAr = emptyList(),
        )
    }
}

object PressureConverterCalculator : FamilyConverter(
    converterDefinition("pressure-converter", "Pressure Converter", UnitFamily.PRESSURE, "bar"),
    UnitFamily.PRESSURE,
    setOf("bar", "kpa", "psi", "mh2o"),
)

object FlowConverterCalculator : FamilyConverter(
    converterDefinition("flow-converter", "Flow Converter", UnitFamily.FLOW, "m3h"),
    UnitFamily.FLOW,
    setOf("m3h", "ls", "cfm", "gpm"),
)

object PowerConverterCalculator : FamilyConverter(
    converterDefinition("power-converter", "Power Converter", UnitFamily.POWER, "kw"),
    UnitFamily.POWER,
    setOf("kw", "hp", "tr", "btuh"),
)

object LengthConverterCalculator : FamilyConverter(
    converterDefinition("length-converter", "Length Converter", UnitFamily.LENGTH, "m"),
    UnitFamily.LENGTH,
    setOf("m", "mm", "in", "ft"),
)

object TemperatureConverterCalculator : FamilyConverter(
    converterDefinition("temperature-converter", "Temperature Converter", UnitFamily.TEMPERATURE, "c"),
    UnitFamily.TEMPERATURE,
    setOf("c", "f", "k"),
)
