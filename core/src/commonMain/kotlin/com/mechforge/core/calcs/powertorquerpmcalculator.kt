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
 * Power ↔ Torque ↔ RPM: T = 9550·P[kW]/N[rpm]. Provide any two, get the third.
 */

private val Def = CalculatorDefinition(
    id = "power-torque-rpm",
    name = "Power ↔ Torque ↔ RPM",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Converts between mechanical power, torque and rotational speed. Enter any two.",
    formulaDisplay = "T = 9550·P[kW]/N[rpm]",
    reference = "Standard rotating-machine relation (P = T·ω; 9550 = 30000/π rounded).",
    notes = "The 9550 constant assumes P in kW, N in rpm, T in N·m. Provide exactly two of the three values.",
    keywords = listOf("torque", "power", "rpm", "speed", "motor", "shaft"),
    inputs = listOf(
        InputSpec("p", "Power", "P", UnitFamily.POWER, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
        InputSpec("t", "Torque", "T", UnitFamily.TORQUE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "nm"),
        InputSpec("n", "Rotational speed", "N", UnitFamily.ROTATIONAL_SPEED, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
    ),
)

object PowerTorqueRpmCalculator : Calculator(Def) {

    private const val K = 9550.0 // 30000/π ≈ 9549.2966 (rounded engineering constant 9550)

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val hasP = has(inputs, "p")
        val hasT = has(inputs, "t")
        val hasN = has(inputs, "n")

        val provided = listOf(hasP, hasT, hasN).count { it }
        if (provided != 2) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "p", "Enter exactly TWO of Power / Torque / Speed — the third is computed."
                    )
                )
            )
        }

        val pKw = if (hasP) value(inputs, "p") / 1000.0 else null
        val tNm = if (hasT) value(inputs, "t") else null
        val rpm = if (hasN) value(inputs, "n") else null

        val results = mutableListOf<com.mechforge.core.engine.ResultValue>()
        val steps = mutableListOf<String>()
        val stepsAr = mutableListOf<String>()

        if (pKw != null && rpm != null) {
            val t = K * pKw / rpm
            results += result("t", "Torque", t, "nm", isPrimary = true)
            results += result("p", "Power", pKw, "kw")
            results += result("n", "Speed", rpm, "rpm")
            steps += "T = 9550·P/N = 9550 × ${Fmt.n(pKw, 3)} / ${Fmt.n(rpm, 1)} = ${Fmt.n(t, 3)} N·m"
            stepsAr += "العزم: T = 9550·P/N = 9550 × ${Fmt.n(pKw, 3)} / ${Fmt.n(rpm, 1)} = ${Fmt.n(t, 3)} N·m"
        } else if (tNm != null && rpm != null) {
            val p = tNm * rpm / K
            results += result("p", "Power", p, "kw", isPrimary = true)
            results += result("t", "Torque", tNm, "nm")
            results += result("n", "Speed", rpm, "rpm")
            steps += "P = T·N/9550 = ${Fmt.n(tNm, 3)} × ${Fmt.n(rpm, 1)} / 9550 = ${Fmt.n(p, 4)} kW"
            stepsAr += "القدرة: P = T·N/9550 = ${Fmt.n(tNm, 3)} × ${Fmt.n(rpm, 1)} / 9550 = ${Fmt.n(p, 4)} kW"
        } else if (pKw != null && tNm != null) {
            if (tNm == 0.0) {
                throw com.mechforge.core.engine.ValidationException(
                    listOf(com.mechforge.core.engine.InputError("t", "Torque must not be zero when solving for speed."))
                )
            }
            val n = K * pKw / tNm
            results += result("n", "Speed", n, "rpm", isPrimary = true)
            results += result("p", "Power", pKw, "kw")
            results += result("t", "Torque", tNm, "nm")
            steps += "N = 9550·P/T = 9550 × ${Fmt.n(pKw, 3)} / ${Fmt.n(tNm, 3)} = ${Fmt.n(n, 1)} rpm"
            stepsAr += "السرعة: N = 9550·P/T = 9550 × ${Fmt.n(pKw, 3)} / ${Fmt.n(tNm, 3)} = ${Fmt.n(n, 1)} rpm"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            stepsAr = stepsAr,
            warnings = emptyList(),
        )
    }

}
