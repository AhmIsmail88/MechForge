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

/** Reynolds number: Re = v·D / ν (kinematic form). */

private val Def = CalculatorDefinition(
    id = "reynolds-number",
    name = "Reynolds Number",
    category = CalculatorCategory.HYDRAULICS,
    description = "Dimensionless ratio of inertial to viscous forces, with flow regime annotation.",
    formulaDisplay = "Re = v·D / ν",
    reference = "Osborne Reynolds (1883); standard fluid mechanics (e.g. White, Fluid Mechanics).",
    notes = "ν is the KINEMATIC viscosity. If you only have dynamic viscosity μ, use ν = μ/ρ.",
    keywords = listOf("reynolds", "laminar", "turbulent", "regime", "viscous"),
    inputs = listOf(
        InputSpec("v", "Velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("nu", "Kinematic viscosity", "ν", UnitFamily.KINEMATIC_VISCOSITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "cst"),
    ),
)

object ReynoldsNumberCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val v = value(inputs, "v") // m/s
        val d = value(inputs, "d") // m
        val nu = value(inputs, "nu") // m²/s

        val re = v * d / nu
        val regime = FrictionFactor.regimeName(re)

        return CalcOutput(
            results = listOf(
                result("re", "Reynolds Number", re, "dash", isPrimary = true),
            ),
            steps = listOf(
                "Re = v·D / ν = ${Fmt.n(v, 3)} × ${Fmt.n(d, 4)} / ${Fmt.n(nu, 9)} = ${Fmt.n(re, 0)}",
                "Flow regime: $regime (laminar < 2300, transitional 2300–4000, turbulent > 4000)",
            ),
            stepsAr = listOf(
                "Re = v·D / ν = ${Fmt.n(v, 3)} × ${Fmt.n(d, 4)} / ${Fmt.n(nu, 9)} = ${Fmt.n(re, 0)}",
                "نظام السريان: " + when {
                    re < FrictionFactor.LAMINAR_LIMIT -> "صفحي"
                    re <= 4000.0 -> "انتقالي"
                    else -> "مضطرب"
                } + " (صفحي < 2300، انتقالي 2300-4000، مضطرب > 4000)",
            ),
            warnings = listOfNotNull(FrictionFactor.regimeWarning(re)),
            warningsAr = buildList {
                // the same condition as FrictionFactor.regimeWarning
                if (re >= 2300.0 && re <= 4000.0) {
                    add("رقم رينولدز في المنطقة الانتقالية (2300-4000)؛ معامل الاحتكاك غير مؤكد فيها - تعامل مع النتيجة بحذر.")
                }
            },
        )
    }

}
