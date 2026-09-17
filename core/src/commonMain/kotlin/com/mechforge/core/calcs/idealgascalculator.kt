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
 * Ideal gas law / gas density: ρ = p·M / (R̄·T), specific volume v = 1/ρ,
 * and mass m = ρ·V when a volume is provided.
 */

private val Def = CalculatorDefinition(
    id = "ideal-gas",
    name = "Ideal Gas Law / Gas Density",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "Gas density and specific volume from pressure, temperature and molar mass; optional mass for a given volume.",
    formulaDisplay = "ρ = p·M / (R̄·T) ;  v = 1/ρ ;  m = ρ·V",
    reference = "Ideal gas law (universal gas constant R̄ = 8.314462618 J/(mol·K)).",
    notes = "Valid when the gas behaves ideally (low pressure, moderate temperature). Air M ≈ 28.965 g/mol.",
    keywords = listOf("ideal gas", "density", "pv=nrt", "specific volume", "thermodynamics"),
    inputs = listOf(
        InputSpec("p", "Absolute pressure", "p", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("t", "Absolute temperature", "T", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "k"),
        InputSpec("m", "Molar mass", "M", UnitFamily.MOLAR_MASS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "gmol"),
        InputSpec("v", "Volume (optional)", "V", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
    ),
)

object IdealGasCalculator : Calculator(Def) {

    private const val R_UNIVERSAL = 8.314462618 // J/(mol·K)

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p") // Pa
        val t = value(inputs, "t") // K
        val m = value(inputs, "m") // kg/mol

        val rho = p * m / (R_UNIVERSAL * t) // kg/m³
        val specificVolume = 1.0 / rho

        val results = mutableListOf(
            result("rho", "Gas Density", rho, "kgm3", isPrimary = true),
            result("sv", "Specific Volume (m³/kg)", specificVolume, "m3perkg"),
        )

        val steps = mutableListOf(
            "ρ = p·M / (R̄·T) = ${Fmt.n(p, 1)} Pa × ${Fmt.n(m * 1000.0, 3)} g/mol / (8.314462618 × ${Fmt.n(t, 2)} K) = ${Fmt.n(rho, 4)} kg/m³",
            "Specific volume: v = 1/ρ = ${Fmt.n(specificVolume, 5)} m³/kg",
        )

        if (has(inputs, "v")) {
            val volume = value(inputs, "v") // m³
            if (volume > 0.0) {
                val mass = rho * volume
                steps += "Mass in ${Fmt.n(volume, 4)} m³: m = ρ·V = ${Fmt.n(mass, 5)} kg"
                results += result("mass", "Gas Mass", mass, "kg")
            }
        }

        val stepsAr = mutableListOf(
            "ρ = p·M / (R̄·T) = ${Fmt.n(p, 1)} Pa × ${Fmt.n(m * 1000.0, 3)} g/mol / (8.314462618 × ${Fmt.n(t, 2)} K) = ${Fmt.n(rho, 4)} kg/m³",
            "الحجم النوعي: v = 1/ρ = ${Fmt.n(specificVolume, 5)} m³/kg",
        )
        if (has(inputs, "v")) {
            val volumeAr = value(inputs, "v")
            if (volumeAr > 0.0) {
                stepsAr += "الكتلة في ${Fmt.n(volumeAr, 4)} m³: m = ρ·V = ${Fmt.n(rho * volumeAr, 5)} kg"
            }
        }

        return CalcOutput(results = results, steps = steps, stepsAr = stepsAr, warnings = emptyList(), warningsAr = emptyList())
    }

}
