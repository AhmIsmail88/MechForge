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

/** Helical compression spring rate: k = G·d⁴/(8·D³·n). */
private val Def = CalculatorDefinition(
    id = "spring-rate",
    name = "Spring Rate (Compression)",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Spring rate of a helical compression spring, with deflection and load at a given force.",
    formulaDisplay = "k = G·d⁴ / (8·D³·n) ;  δ = F/k",
    reference = "Standard helical-spring relations (e.g. Shigley, Mechanical Engineering Design).",
    notes = "d = wire diameter, D = mean coil diameter, n = number of ACTIVE coils. G ≈ 79.3 GPa for steel wire. Fatigue, buckling, end conditions and solid height are not covered.",
    keywords = listOf("spring", "rate", "stiffness", "compression", "coil", "deflection"),
    inputs = listOf(
        InputSpec("g", "Shear modulus", "G", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa", assumedWhenOmitted = "Shear modulus assumed as 79.3 GPa (steel) - use the value for the actual spring material."),
        InputSpec("d", "Wire diameter", "d", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("dm", "Mean coil diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("n", "Active coils", "n", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("f", "Applied force (optional)", "F", UnitFamily.FORCE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "n"),
    ),
)

object SpringRateCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val g = optionalValue(inputs, "g", 79.3e9)
        val d = value(inputs, "d")
        val mean = value(inputs, "dm")
        val coils = value(inputs, "n")

        if (mean <= d) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("dm", "Mean coil diameter must be larger than the wire diameter."))
            )
        }

        val k = g * d.pow(4) / (8.0 * mean.pow(3) * coils)

        val results = mutableListOf(
            result("k", "Spring Rate", k, "nperm", isPrimary = true),
            result("kmm", "Spring Rate per mm", k / 1000.0, "npermm", isPrimary = true),
        )
        val steps = mutableListOf(
            "d⁴ = ${Fmt.n(d * 1000.0, 2)}⁴ mm⁴ = ${Fmt.n(d.pow(4), 12)} m⁴ ; D³ = ${Fmt.n(mean * 1000.0, 2)}³ mm³",
            "k = G·d⁴/(8·D³·n) = ${Fmt.n(g / 1e9, 1)} GPa × ${Fmt.n(d.pow(4), 12)} / (8 × ${Fmt.n(mean.pow(3), 9)} × ${Fmt.n(coils, 1)}) = ${Fmt.n(k, 1)} N/m = ${Fmt.n(k / 1000.0, 3)} N/mm",
        )

        if (has(inputs, "f")) {
            val f = value(inputs, "f")
            val deflection = f / k
            results += result("delta", "Deflection", deflection * 1000.0, "mm", isPrimary = true)
            steps += "Deflection: δ = F/k = ${Fmt.n(f, 1)} N / ${Fmt.n(k, 1)} N/m = ${Fmt.n(deflection * 1000.0, 2)} mm"
        }

        val springIndex = mean / d

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "g")) add("Shear modulus not provided — assumed 79.3 GPa (steel wire).")
                if (springIndex < 4.0 || springIndex > 12.0) {
                    add("Spring index D/d = ${Fmt.n(springIndex, 2)} is outside the usual 4–12 range; check manufacturability and stress concentration.")
                }
            },
        )
    }
}
