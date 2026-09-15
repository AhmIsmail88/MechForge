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

/** Pipe mass per unit length: W = π·(OD − t)·t·ρ. */
private val Def = CalculatorDefinition(
    id = "pipe-weight",
    name = "Pipe Weight per Metre",
    category = CalculatorCategory.PIPING,
    description = "Mass per metre of a straight pipe from outside diameter, wall thickness and material density.",
    formulaDisplay = "W = π·(OD − t)·t·ρ",
    reference = "Geometric section-area relation; check against the applicable pipe standard or mill data.",
    notes = "Excludes coatings, lining, insulation and contents. Density defaults to 7850 kg/m³ (carbon steel).",
    keywords = listOf("pipe weight", "mass", "wall thickness", "od", "steel"),
    inputs = listOf(
        InputSpec("od", "Outside diameter", "OD", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("t", "Wall thickness", "t", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("rho", "Material density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
    ),
)

object PipeWeightCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val od = value(inputs, "od")
        val t = value(inputs, "t")
        val rho = optionalValue(inputs, "rho", 7850.0)

        if (t >= od / 2.0) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("t", "Wall thickness must be less than half the outside diameter."))
            )
        }

        val area = PI * (od - t) * t
        val weight = area * rho

        return CalcOutput(
            results = listOf(
                result("w", "Mass per Metre (kg/m)", weight, "kgperm", isPrimary = true),
                result("area", "Metal Cross-Section Area", area, "m2"),
                result("id", "Internal Diameter", (od - 2.0 * t) * 1000.0, "mm"),
            ),
            steps = listOf(
                "Metal area: A = π·(OD − t)·t = π × (${Fmt.n(od * 1000.0, 1)} − ${Fmt.n(t * 1000.0, 2)}) mm × ${Fmt.n(t * 1000.0, 2)} mm = ${Fmt.n(area, 8)} m²",
                "Mass per metre: W = A·ρ = ${Fmt.n(area, 8)} × ${Fmt.n(rho, 1)} = ${Fmt.n(weight, 2)} kg/m",
                "Internal diameter: ID = OD − 2t = ${Fmt.n((od - 2.0 * t) * 1000.0, 1)} mm",
            ),
            warnings = if (!has(inputs, "rho")) {
                listOf("Density not provided — assumed 7850 kg/m³ (carbon steel). Result labelled kg/m.")
            } else {
                listOf("Result is mass per metre (kg/m) for the entered density.")
            },
        )
    }
}
