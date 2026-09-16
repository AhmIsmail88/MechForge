package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Equivalent length of a fitting from its K value: L_eq = K·D/f. */
private val Def = CalculatorDefinition(
    id = "equivalent-length",
    name = "Equivalent Length (Fittings)",
    category = CalculatorCategory.PIPING,
    description = "Equivalent straight-pipe length of a fitting or valve from its loss coefficient and friction factor.",
    formulaDisplay = "L_eq = K·D / f",
    reference = "Equivalence between local-loss and friction-loss formulations (Crane TP-410 style practice).",
    notes = "f should be the friction factor at the operating Reynolds number. K values come from manufacturer data or reference tables (not embedded).",
    keywords = listOf("equivalent length", "fitting", "valve", "k value", "piping"),
    inputs = listOf(
        InputSpec("k", "Loss coefficient", "K", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("f", "Friction factor", "f", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object EquivalentLengthCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val k = value(inputs, "k")
        val d = value(inputs, "d")
        val f = value(inputs, "f")

        val leq = k * d / f

        return CalcOutput(
            results = listOf(
                result("leq", "Equivalent Length", leq, "m", isPrimary = true),
                result("inD", "Equivalent Length in Diameters", leq / d, "dash"),
            ),
            steps = listOf(
                "L_eq = K·D/f = ${Fmt.n(k, 3)} × ${Fmt.n(d * 1000.0, 1)} mm / ${Fmt.n(f, 5)} = ${Fmt.n(leq, 3)} m",
                "In pipe diameters: L_eq/D = ${Fmt.n(leq / d, 1)} D",
            ),
            stepsAr = listOf(
                "L_eq = K·D/f = ${Fmt.n(k, 3)} × ${Fmt.n(d * 1000.0, 1)} mm / ${Fmt.n(f, 5)} = ${Fmt.n(leq, 3)} m",
                "بالأقطار: L_eq/D = ${Fmt.n(leq / d, 1)} D",
            ),
            warnings = emptyList(),
        )
    }
}
