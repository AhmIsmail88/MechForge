package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.abs
import kotlin.math.exp

/** Heat exchanger effectiveness from NTU and the capacity ratio. */
private val Def = CalculatorDefinition(
    id = "hx-effectiveness-ntu",
    name = "Heat Exchanger Effectiveness (e-NTU)",
    category = CalculatorCategory.EQUIPMENT,
    description = "Effectiveness of a heat exchanger from NTU and the heat-capacity ratio, for counter-current or parallel-flow arrangements.",
    formulaDisplay = "counter: e = (1-exp(-NTU(1-Cr)))/(1-Cr*exp(-NTU(1-Cr))) ;  parallel: e = (1-exp(-NTU(1+Cr)))/(1+Cr)",
    reference = "e-NTU method (Kays & London); closed forms as tabulated in Incropera & DeWitt.",
    notes = "NTU = U*A/C_min and Cr = C_min/C_max (0 <= Cr <= 1). Cr = 0 means one stream changes phase (condenser/evaporator). For Cr = 1 in counter-flow the limit e = NTU/(1+NTU) is used.",
    keywords = listOf("effectiveness", "ntu", "heat exchanger", "kays london", "equipment", "thermal"),
    inputs = listOf(
        InputSpec("ntu", "Number of transfer units", "NTU", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("cr", "Capacity ratio", "Cr", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = false, maxValue = 1.0, defaultUnitId = "dash", assumedWhenOmitted = "Capacity ratio assumed as 0 - this models one stream condensing or evaporating at constant temperature."),
        InputSpec("arr", "Arrangement (1 = counter, 0 = parallel)", "arr", UnitFamily.DIMENSIONLESS, required = false, defaultUnitId = "dash", assumedWhenOmitted = "Flow arrangement assumed as counter-flow - check it against the exchanger actually specified."),
    ),
)

object HxEffectivenessNtuCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val ntu = value(inputs, "ntu")
        val cr = optionalValue(inputs, "cr", 0.0)
        val counter = optionalValue(inputs, "arr", 1.0) >= 0.5

        val eps = if (abs(cr) < 1e-9) {
            1.0 - exp(-ntu)
        } else if (counter && abs(1.0 - cr) < 1e-9) {
            ntu / (1.0 + ntu)
        } else if (counter) {
            (1.0 - exp(-ntu * (1.0 - cr))) / (1.0 - cr * exp(-ntu * (1.0 - cr)))
        } else {
            (1.0 - exp(-ntu * (1.0 + cr))) / (1.0 + cr)
        }

        return CalcOutput(
            results = listOf(
                result("eps", "Effectiveness", eps * 100.0, "pct", isPrimary = true),
                result("epsFrac", "Effectiveness (fraction)", eps, "dash"),
                result("ntuUsed", "NTU", ntu, "dash"),
                result("crUsed", "Capacity Ratio", cr, "dash"),
            ),
            steps = listOf(
                "NTU = ${Fmt.n(ntu, 3)}   Cr = ${Fmt.n(cr, 3)}   Arrangement: ${if (counter) "counter-current" else "parallel-flow"}",
                if (abs(cr) < 1e-9) {
                    "Cr = 0 (phase change): e = 1 - exp(-NTU) = ${Fmt.n(eps, 5)}"
                } else if (counter && abs(1.0 - cr) < 1e-9) {
                    "Cr = 1, counter-flow limit: e = NTU/(1+NTU) = ${Fmt.n(eps, 5)}"
                } else if (counter) {
                    "e = (1-exp(-NTU(1-Cr)))/(1-Cr*exp(-NTU(1-Cr))) = ${Fmt.n(eps, 5)}"
                } else {
                    "e = (1-exp(-NTU(1+Cr)))/(1+Cr) = ${Fmt.n(eps, 5)}"
                },
                "Effectiveness = ${Fmt.n(eps * 100.0, 2)} %",
            ),
            warnings = buildList {
                if (counter && cr > 0.95 && cr < 1.0) add("Cr close to 1: the counter-flow result uses a near-singular form - check the limit value.")
                if (eps > 0.95) add("Effectiveness above 95 % needs a very large surface - check the area and the approach temperatures.")
            },
        )
    }
}
