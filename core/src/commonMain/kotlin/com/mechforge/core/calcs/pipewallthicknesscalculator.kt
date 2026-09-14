package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Minimum pressure-design wall thickness (hoop stress) plus corrosion allowance. */
private val Def = CalculatorDefinition(
    id = "pipe-wall-thickness",
    name = "Pipe Wall Thickness (Pressure Design)",
    category = CalculatorCategory.PIPING,
    description = "Minimum wall thickness for internal pressure using the hoop-stress (Barlow-style) relation, plus corrosion allowance.",
    formulaDisplay = "t = p·D / (2·σ_allow) + CA",
    reference = "Hoop-stress basis (Barlow); design codes such as ASME B31.3 add coefficients, tolerances and joint factors.",
    notes = "This is a PRELIMINARY calculation only. Design codes require additional factors (E, W, Y, mill tolerance, corrosion allowance rules). Do not use as sole basis for procurement or fabrication.",
    keywords = listOf("wall thickness", "pressure design", "hoop stress", "barlow", "pipe"),
    inputs = listOf(
        InputSpec("p", "Design pressure", "p", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("d", "Outside diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("sigma", "Allowable stress", "σ_allow", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("ca", "Corrosion allowance", "CA", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "mm"),
    ),
)

object PipeWallThicknessCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p")
        val d = value(inputs, "d")
        val sigma = value(inputs, "sigma")
        val ca = optionalValue(inputs, "ca", 0.0)

        val tPressure = p * d / (2.0 * sigma)
        val tTotal = tPressure + ca

        return CalcOutput(
            results = listOf(
                result("tp", "Pressure Design Thickness", tPressure * 1000.0, "mm"),
                result("t", "Total Minimum Thickness (incl. CA)", tTotal * 1000.0, "mm", isPrimary = true),
            ),
            steps = listOf(
                "Pressure thickness: t_p = p·D/(2·σ_allow) = ${Fmt.n(p / 1e5, 3)} bar × ${Fmt.n(d * 1000.0, 1)} mm / (2 × ${Fmt.n(sigma / 1e6, 1)} MPa) = ${Fmt.n(tPressure * 1000.0, 3)} mm",
                "Corrosion allowance: ${Fmt.n(ca * 1000.0, 2)} mm",
                "Total: t = ${Fmt.n(tPressure * 1000.0, 3)} + ${Fmt.n(ca * 1000.0, 2)} = ${Fmt.n(tTotal * 1000.0, 2)} mm",
            ),
            warnings = listOf(
                "PRELIMINARY ONLY — the applicable design code (e.g. ASME B31.3 / B31.1) requires additional coefficients, mill tolerance and joint factors.",
                "Manufacturing minima and handling thickness must also be checked.",
            ),
        )
    }
}
