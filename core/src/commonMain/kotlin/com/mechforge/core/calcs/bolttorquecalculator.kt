package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Bolt tightening torque / preload: T = K·F·d (and tensile stress from the stress area). */
private val Def = CalculatorDefinition(
    id = "bolt-torque",
    name = "Bolt Torque & Preload",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Tightening torque from preload (or preload from torque), plus bolt tensile stress when the stress area is given.",
    formulaDisplay = "T = K·F·d ;  F = T/(K·d) ;  σ = F/A_t",
    reference = "Standard threaded-fastener torque-preload relation (K = nut factor, e.g. Shigley; VDI 2230).",
    notes = "K = 0.20 is typical for as-received steel bolts with mineral oil. Lubrication and coatings strongly affect K — verify for critical joints. σ uses the bolt tensile stress area A_t.",
    keywords = listOf("bolt", "torque", "preload", "tension", "fastener", "stress area"),
    inputs = listOf(
        InputSpec("k", "Nut factor", "K", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("d", "Nominal bolt diameter", "d", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("f", "Preload (bolt tension)", "F", UnitFamily.FORCE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("t", "Tightening torque", "T", UnitFamily.TORQUE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "nm"),
        InputSpec("at", "Bolt tensile stress area", "A_t", UnitFamily.AREA, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm2"),
    ),
)

object BoltTorqueCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val k = optionalValue(inputs, "k", 0.20)
        val d = value(inputs, "d")
        val hasF = has(inputs, "f")
        val hasT = has(inputs, "t")

        if (hasF == hasT) {
            throw ValidationException(
                listOf(InputError("f", "Enter EITHER the preload (F) OR the tightening torque (T) — not both, not neither."))
            )
        }

        val force: Double
        val torque: Double
        if (hasF) {
            force = value(inputs, "f")
            torque = k * force * d
        } else {
            torque = value(inputs, "t")
            force = torque / (k * d)
        }

        val results = mutableListOf(
            result("t", "Tightening Torque", torque, "nm", isPrimary = true),
            result("f", "Preload (Bolt Tension)", force / 1000.0, "kn", isPrimary = true),
        )
        val steps = mutableListOf(
            "Nut factor K = ${Fmt.n(k, 3)}, diameter d = ${Fmt.n(d * 1000.0, 2)} mm",
            if (hasF) {
                "Torque: T = K·F·d = ${Fmt.n(k, 3)} × ${Fmt.n(force / 1000.0, 2)} kN × ${Fmt.n(d * 1000.0, 2)} mm = ${Fmt.n(torque, 2)} N·m"
            } else {
                "Preload: F = T/(K·d) = ${Fmt.n(torque, 2)} / (${Fmt.n(k, 3)} × ${Fmt.n(d * 1000.0, 2)} mm) = ${Fmt.n(force / 1000.0, 2)} kN"
            },
        )

        if (has(inputs, "at")) {
            val at = value(inputs, "at")
            val sigma = force / at
            results += result("sigma", "Bolt Tensile Stress", sigma / 1e6, "mpa")
            steps += "Tensile stress: σ = F/A_t = ${Fmt.n(force / 1000.0, 2)} kN / ${Fmt.n(at * 1e6, 2)} mm² = ${Fmt.n(sigma / 1e6, 1)} MPa"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = if (!has(inputs, "k")) listOf("Nut factor not provided — assumed 0.20 (as-received steel, lightly oiled).") else emptyList(),
        )
    }
}
