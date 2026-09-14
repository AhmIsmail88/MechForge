package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.pow

/** Isentropic (adiabatic reversible) relations for an ideal gas. */
private val Def = CalculatorDefinition(
    id = "isentropic-relation",
    name = "Isentropic Relations (Ideal Gas)",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "Temperature ratio for an isentropic pressure change of an ideal gas, with the density ratio.",
    formulaDisplay = "T₂/T₁ = (P₂/P₁)^((k−1)/k) ;  ρ₂/ρ₁ = (P₂/P₁)^(1/k)",
    reference = "Ideal-gas isentropic relations (e.g. Moran & Shapiro, Fundamentals of Engineering Thermodynamics).",
    notes = "k = c_p/c_v (1.4 for air at ambient conditions). Assumes reversible adiabatic (isentropic) compression or expansion.",
    keywords = listOf("isentropic", "adiabatic", "ideal gas", "compression", "expansion", "k"),
    inputs = listOf(
        InputSpec("p1", "Inlet pressure", "P₁", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("p2", "Outlet pressure", "P₂", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("t1", "Inlet temperature", "T₁", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
        InputSpec("k", "Specific heat ratio", "k", UnitFamily.DIMENSIONLESS, required = false, minValue = 1.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object IsentropicRelationCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p1 = value(inputs, "p1")
        val p2 = value(inputs, "p2")
        val t1 = value(inputs, "t1")
        val k = optionalValue(inputs, "k", 1.4)

        val pressureRatio = p2 / p1
        val t2 = t1 * pressureRatio.pow((k - 1.0) / k)
        val densityRatio = pressureRatio.pow(1.0 / k)

        return CalcOutput(
            results = listOf(
                result("t2", "Outlet Temperature", t2, "k", isPrimary = true),
                result("t2c", "Outlet Temperature (°C)", t2 - 273.15, "c"),
                result("tratio", "Temperature Ratio T₂/T₁", t2 / t1, "dash"),
                result("dratio", "Density Ratio ρ₂/ρ₁", densityRatio, "dash"),
                result("pratio", "Pressure Ratio P₂/P₁", pressureRatio, "dash"),
            ),
            steps = listOf(
                "Pressure ratio: P₂/P₁ = ${Fmt.n(p2, 2)} / ${Fmt.n(p1, 2)} = ${Fmt.n(pressureRatio, 4)}",
                "Exponent (k−1)/k = (${Fmt.n(k, 3)} − 1)/${Fmt.n(k, 3)} = ${Fmt.n((k - 1.0) / k, 4)}",
                "T₂ = T₁·(P₂/P₁)^((k−1)/k) = ${Fmt.n(t1, 2)} × ${Fmt.n(pressureRatio, 4)}^${Fmt.n((k - 1.0) / k, 4)} = ${Fmt.n(t2, 2)} K (${Fmt.n(t2 - 273.15, 1)} °C)",
                "Density ratio: ρ₂/ρ₁ = (P₂/P₁)^(1/k) = ${Fmt.n(densityRatio, 4)}",
            ),
            warnings = if (!has(inputs, "k")) listOf("Specific heat ratio not provided — assumed k = 1.4 (air).") else emptyList(),
        )
    }
}
