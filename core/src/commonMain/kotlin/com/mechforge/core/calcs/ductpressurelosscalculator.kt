package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.math.FrictionFactor
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Duct friction pressure loss with Darcy-Weisbach on the hydraulic diameter. */
private val Def = CalculatorDefinition(
    id = "duct-pressure-loss",
    name = "Duct Friction Pressure Loss",
    category = CalculatorCategory.HVAC,
    description = "Straight-duct friction pressure loss using Darcy-Weisbach with the duct hydraulic diameter and Colebrook friction factor.",
    formulaDisplay = "Δp = f·(L/D_h)·ρ·v²/2 ;  D_h = D (round) = 2WH/(W+H) (rectangular)",
    reference = "Darcy-Weisbach; Colebrook-White friction factor; duct hydraulic-diameter convention (ASHRAE-style).",
    notes = "Air density defaults to 1.2 kg/m³, kinematic viscosity to 1.5e-5 m²/s (~20 °C), roughness to 0.09 mm (galvanised steel). Fittings and terminal losses are NOT included.",
    keywords = listOf("duct", "pressure loss", "friction", "darcy", "hvac", "air"),
    inputs = listOf(
        InputSpec("v", "Air velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("l", "Duct length", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("d", "Round duct diameter", "D", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("w", "Rectangular width", "W", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("h", "Rectangular height", "H", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("rho", "Air density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density", assumedWhenOmitted = "Air density assumed as 1.2 kg/m3 (20 C, sea level) - correct it for the actual temperature and altitude."),
        InputSpec("nu", "Kinematic viscosity", "ν", UnitFamily.KINEMATIC_VISCOSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "cst", assumedWhenOmitted = "Kinematic viscosity assumed as 1.5e-5 m2/s (air at 20 C) - correct it for the actual temperature."),
        InputSpec("eps", "Absolute roughness", "ε", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "mm", libraryKey = "roughness", assumedWhenOmitted = "Absolute roughness assumed as 9e-5 m (galvanised steel) - use the value for the actual duct material."),
    ),
)

object DuctPressureLossCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val v = value(inputs, "v")
        val l = value(inputs, "l")
        val rho = optionalValue(inputs, "rho", 1.2)
        val nu = optionalValue(inputs, "nu", 1.5e-5)
        val eps = optionalValue(inputs, "eps", 9e-5)

        val hasRound = has(inputs, "d")
        val hasRect = has(inputs, "w") && has(inputs, "h")
        if (!hasRound && !hasRect) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "d", "Provide the round duct diameter, OR both rectangular width and height."
                    )
                )
            )
        }

        val dh = if (hasRound) {
            value(inputs, "d")
        } else {
            val w = value(inputs, "w")
            val h = value(inputs, "h")
            2.0 * w * h / (w + h)
        }

        val re = v * dh / nu
        val relRough = eps / dh
        val f = FrictionFactor.darcy(re, relRough)
        val velocityPressure = rho * v * v / 2.0
        val dp = f * (l / dh) * velocityPressure

        val warnings = buildList {
            FrictionFactor.regimeWarning(re)?.let { add(it) }
            add("Uses the straight-duct fiction only — add fitting/terminal losses for a full system total.")
            if (dp / l > 1.5) add("Friction loss above 1.5 Pa/m - above the usual design band; check the fan energy against the project criterion.")
            if (dp / l < 0.5) add("Friction loss below 0.5 Pa/m - the duct may be oversized for the stated criterion.")
        }

        return CalcOutput(
            results = listOf(
                result("dp", "Friction Pressure Loss", dp, "pa", isPrimary = true),
                result("dpPerM", "Loss per metre", dp / l, "pam"),
                result("f", "Friction Factor", f, "dash"),
                result("dh", "Hydraulic Diameter", dh * 1000.0, "mm"),
                result("re", "Reynolds Number", re, "dash"),
            ),
            steps = listOf(
                "Hydraulic diameter: D_h = ${Fmt.n(dh, 4)} m" + if (hasRound) " (round duct)" else " (rectangular 2WH/(W+H))",
                "Reynolds number: Re = v·D_h/ν = ${Fmt.n(v, 3)} × ${Fmt.n(dh, 4)} / ${Fmt.n(nu, 8)} = ${Fmt.n(re, 0)}",
                "Relative roughness: ε/D_h = ${Fmt.n(eps * 1000.0, 3)} mm / ${Fmt.n(dh * 1000.0, 1)} mm = ${Fmt.n(relRough, 6)}",
                "Friction factor (Colebrook-White): f = ${Fmt.n(f, 5)}",
                "Velocity pressure: ρv²/2 = ${Fmt.n(rho, 3)} × ${Fmt.n(v, 3)}² / 2 = ${Fmt.n(velocityPressure, 3)} Pa",
                "Pressure loss: Δp = f·(L/D_h)·ρv²/2 = ${Fmt.n(f, 5)} × (${Fmt.n(l, 2)}/${Fmt.n(dh, 4)}) × ${Fmt.n(velocityPressure, 3)} = ${Fmt.n(dp, 2)} Pa",
            ),
            warnings = warnings,
        )
    }
}
