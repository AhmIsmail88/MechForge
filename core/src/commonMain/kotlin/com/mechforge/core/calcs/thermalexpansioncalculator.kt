package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Thermal expansion of a pipe run: ΔL = α·L·ΔT. */
private val Def = CalculatorDefinition(
    id = "thermal-expansion",
    name = "Pipe Thermal Expansion",
    category = CalculatorCategory.PIPING,
    description = "Free thermal expansion of a pipe run between installation and operating temperature.",
    formulaDisplay = "ΔL = α·L·ΔT",
    reference = "Linear thermal expansion; α values from material data (reference values — verify for your material).",
    notes = "α defaults to 12 µm/(m·K) (carbon steel). Free expansion only — restraint, anchors and expansion loops are a separate design step.",
    keywords = listOf("thermal expansion", "pipe", "expansion loop", "temperature", "stress"),
    inputs = listOf(
        InputSpec("alpha", "Expansion coefficient", "α", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "permk"),
        InputSpec("l", "Pipe run length", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("dt", "Temperature change", "ΔT", UnitFamily.TEMPERATURE_DIFFERENCE, defaultUnitId = "delc"),
    ),
)

object ThermalExpansionCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val alpha = optionalValue(inputs, "alpha", 12e-6)
        val l = value(inputs, "l")
        val dt = value(inputs, "dt")

        val dl = alpha * l * dt
        val expansionPerMetre = alpha * dt

        return CalcOutput(
            results = listOf(
                result("dl", "Free Expansion", dl * 1000.0, "mm", isPrimary = true),
                result("perM", "Expansion per Metre", expansionPerMetre * 1000.0, "mm", isPrimary = true),
            ),
            steps = listOf(
                "α = ${Fmt.n(alpha / 1e-6, 1)} µm/(m·K) = ${Fmt.n(alpha, 8)} 1/K",
                "ΔL = α·L·ΔT = ${Fmt.n(alpha, 8)} × ${Fmt.n(l, 2)} m × ${Fmt.n(dt, 1)} K = ${Fmt.n(dl * 1000.0, 2)} mm",
                "Per metre: ${Fmt.n(expansionPerMetre * 1000.0, 3)} mm/m",
            ),
            warnings = buildList {
                if (!has(inputs, "alpha")) add("Expansion coefficient not provided — assumed 12 µm/(m·K) (carbon steel).")
                if (dl > 0.025) {
                    add("Total expansion exceeds 25 mm — expansion loops, anchors or expansion joints are likely required. Check the applicable design basis.")
                }
            },
            stepsAr = listOf(
                "α = ${Fmt.n(alpha / 1e-6, 1)} µm/(m·K) = ${Fmt.n(alpha, 8)} 1/K",
                "ΔL = α·L·ΔT = ${Fmt.n(alpha, 8)} × ${Fmt.n(l, 2)} m × ${Fmt.n(dt, 1)} K = ${Fmt.n(dl * 1000.0, 2)} mm",
                "لكل متر من الماسورة: ${Fmt.n(expansionPerMetre * 1000.0, 3)} mm/m",
            ),
            warningsAr = buildList {
                if (!has(inputs, "alpha")) {
                    add("لم يُدخل معامل التمدد - افتُرض 12 µm/(m·K) (صلب كربوني).")
                }
                if (dl > 0.025) {
                    add("التمدد الكلي يتجاوز 25 mm - غالبًا ستلزم حلقات تمدد أو مثبتات أو وصلات تمدد. راجع أساس التصميم المطبق.")
                }
            },
        )
    }
}
