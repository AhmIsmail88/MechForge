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

/** Pipe mass per unit length: W = PI*(OD - t)*t*rho. */
private val Def = CalculatorDefinition(
    id = "pipe-weight",
    name = "Pipe Weight per Metre",
    category = CalculatorCategory.PIPING,
    description = "Mass per metre of a straight pipe from outside diameter, wall thickness and material density, with the option of the operating (filled) weight.",
    formulaDisplay = "W = PI*(OD - t)*t*rho   ;   W_content = PI*ID^2/4 * rho_content",
    reference = "Geometric section-area relation; check against the applicable pipe standard or mill data.",
    notes = "Excludes coatings, lining, insulation and fittings. Density defaults to 7850 kg/m3 (carbon steel). Enter a content density (e.g. 1000 kg/m3 for water) to get the operating weight used for supports and hydrotest.",
    keywords = listOf("pipe weight", "mass", "wall thickness", "od", "steel", "water filled", "operating weight"),
    inputs = listOf(
        InputSpec("od", "Outside diameter", "OD", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("t", "Wall thickness", "t", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("rho", "Material density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
        InputSpec("rhoc", "Content density (for the operating weight)", "rho_c", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
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
        val boreDiameter = od - 2.0 * t
        val contentArea = PI * boreDiameter * boreDiameter / 4.0
        val hasContent = has(inputs, "rhoc")
        val contentWeight = if (hasContent) contentArea * value(inputs, "rhoc") else 0.0
        val totalWeight = weight + contentWeight

        val results = mutableListOf(
            result("w", "Mass per Metre (kg/m)", weight, "kgperm", isPrimary = true),
            result("area", "Metal Cross-Section Area", area, "m2"),
            result("id", "Internal Diameter", boreDiameter * 1000.0, "mm"),
        )
        if (hasContent) {
            results += result("wc", "Content Mass per Metre (kg/m)", contentWeight, "kgperm")
            results += result("wtot", "Total Operating Mass per Metre (kg/m)", totalWeight, "kgperm", isPrimary = true)
        }

        val steps = mutableListOf(
            "Metal area: A = PI*(OD - t)*t = PI x (${Fmt.n(od * 1000.0, 1)} - ${Fmt.n(t * 1000.0, 2)}) mm x ${Fmt.n(t * 1000.0, 2)} mm = ${Fmt.n(area, 8)} m2",
            "Mass per metre: W = A*rho = ${Fmt.n(area, 8)} x ${Fmt.n(rho, 1)} = ${Fmt.n(weight, 2)} kg/m",
            "Internal diameter: ID = OD - 2t = ${Fmt.n(boreDiameter * 1000.0, 1)} mm",
        )
        if (hasContent) {
            steps += "Content per metre: A_id x rho_c = ${Fmt.n(contentArea, 8)} m2 x ${Fmt.n(value(inputs, "rhoc"), 1)} = ${Fmt.n(contentWeight, 2)} kg/m"
            steps += "Operating mass: ${Fmt.n(weight, 2)} + ${Fmt.n(contentWeight, 2)} = ${Fmt.n(totalWeight, 2)} kg/m"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 7850 kg/m3 (carbon steel). Result labelled kg/m.")
                if (!hasContent) add("Enter a content density to get the operating (filled) weight needed for supports and hydrotest.")
                add("Coatings, lining, insulation and fittings are excluded; add them for the installed weight.")
            },
        )
    }
}
