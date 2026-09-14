package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.sqrt

/** Valve flow coefficient (liquid sizing): Q = Kv·√(ΔP/SG). */
private val Def = CalculatorDefinition(
    id = "valve-kv",
    name = "Valve Flow Coefficient (Kv / Cv)",
    category = CalculatorCategory.PIPING,
    description = "Liquid flow through a valve from its Kv, pressure drop and specific gravity; Cv reported for reference.",
    formulaDisplay = "Q[m³/h] = Kv·√(ΔP[bar]/SG) ;  Cv = 1.156·Kv",
    reference = "IEC 60534 / standard valve-sizing relations.",
    notes = "Liquid service only, no cavitation/flashing check. For gases and steam use the standard's compressible-flow equations.",
    keywords = listOf("valve", "kv", "cv", "flow coefficient", "control valve", "sizing"),
    inputs = listOf(
        InputSpec("kv", "Flow coefficient", "Kv", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("dp", "Pressure drop across valve", "ΔP", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("sg", "Specific gravity (water = 1)", "SG", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object ValveKvCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val kv = value(inputs, "kv")
        val dpPa = value(inputs, "dp")
        val sg = optionalValue(inputs, "sg", 1.0)

        val dpBar = dpPa / 1e5
        val qM3h = kv * sqrt(dpBar / sg)
        val cv = 1.156 * kv

        return CalcOutput(
            results = listOf(
                result("q", "Liquid Flow Rate", qM3h, "m3h", isPrimary = true),
                result("cv", "Equivalent Cv (US)", cv, "dash"),
                result("dp", "Pressure Drop", dpBar, "bar"),
            ),
            steps = listOf(
                "Pressure drop: ΔP = ${Fmt.n(dpPa, 1)} Pa = ${Fmt.n(dpBar, 3)} bar",
                "Flow: Q = Kv·√(ΔP/SG) = ${Fmt.n(kv, 3)} × √(${Fmt.n(dpBar, 3)}/${Fmt.n(sg, 3)}) = ${Fmt.n(qM3h, 3)} m³/h",
                "Equivalent Cv = 1.156 × Kv = ${Fmt.n(cv, 3)}",
            ),
            warnings = buildList {
                if (!has(inputs, "sg")) add("Specific gravity not provided — assumed 1.0 (water).")
                add("Liquid sizing only — no cavitation, flashing, laminar-flow or gas/steam correction applied.")
            },
        )
    }
}
