package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.PI
import kotlin.math.pow

/**
 * Torsional shear stress in a solid circular shaft: τ = 16·T / (π·d³).
 * Given T and allowable τ, solves the minimum solid diameter.
 */

private val Def = CalculatorDefinition(
    id = "torsional-stress",
    name = "Torsional Shear Stress",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Torsional shear stress in a solid circular shaft, or minimum diameter for an allowable stress.",
    formulaDisplay = "τ = 16·T / (π·d³) ;  d_min = (16·T/(π·τ_allow))^(1/3)",
    reference = "Standard strength of materials (e.g. Shigley, Mechanical Engineering Design).",
    notes = "Solid circular shaft only. Bending, keys, notches and fatigue are NOT covered — apply appropriate safety factors.",
    keywords = listOf("torsion", "shear", "shaft", "stress", "diameter", "torque"),
    inputs = listOf(
        InputSpec("t", "Torque", "T", UnitFamily.TORQUE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "nm"),
        InputSpec("d", "Shaft diameter", "d", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("taual", "Allowable shear stress", "τ_allow", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
    ),
)

object TorsionalStressCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val t = value(inputs, "t") // N·m
        val hasD = has(inputs, "d")
        val hasTauAllow = has(inputs, "taual")

        if (!hasD && !hasTauAllow) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "d", "Provide the shaft diameter to compute stress, OR an allowable stress to compute the minimum diameter."
                    )
                )
            )
        }

        val results = mutableListOf<com.mechforge.core.engine.ResultValue>()
        val steps = mutableListOf<String>()

        if (hasD) {
            val d = value(inputs, "d") // m
            val tau = 16.0 * t / (PI * d.pow(3)) // Pa
            results += result("tau", "Torsional Shear Stress", tau / 1e6, "mpa", isPrimary = true)
            steps += "τ = 16·T/(π·d³) = 16 × ${Fmt.n(t, 2)} / (π × ${Fmt.n(d * 1000.0, 2)}³ mm) = ${Fmt.n(tau / 1e6, 2)} MPa"
            if (hasTauAllow) {
                val taual = value(inputs, "taual") // Pa
                val ratio = tau / taual
                results += result("util", "Stress / Allowable", ratio, "dash")
                steps += "Utilization: τ/τ_allow = ${Fmt.n(ratio, 3)}" +
                    (if (ratio > 1.0) "  → EXCEEDS allowable — increase diameter." else "  → within allowable.")
            }
        }

        if (hasTauAllow) {
            val taual = value(inputs, "taual")
            val dMin = (16.0 * t / (PI * taual)).pow(1.0 / 3.0)
            results += result("dmin", "Minimum Solid Shaft Diameter", dMin * 1000.0, "mm", isPrimary = !hasD, isRecommended = !hasD)
            steps += "d_min = (16·T/(π·τ_allow))^(1/3) = ${Fmt.n(dMin * 1000.0, 2)} mm"
        }

        return CalcOutput(results = results, steps = steps, warnings = emptyList())
    }

}
