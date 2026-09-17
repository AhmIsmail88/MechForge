package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.engine.InputError
import com.mechforge.core.math.FrictionFactor
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Friction factor from Reynolds number and relative roughness (Colebrook-White; 64/Re in laminar flow). */
private val Def = CalculatorDefinition(
    id = "friction-factor",
    name = "Friction Factor (Colebrook-White)",
    category = CalculatorCategory.HYDRAULICS,
    description = "Darcy friction factor from Reynolds number, pipe diameter and absolute roughness.",
    formulaDisplay = "1/√f = −2·log₁₀( ε/(3.7·D) + 2.51/(Re·√f) ) ; laminar: f = 64/Re",
    reference = "Colebrook-White (1939); Swamee-Jain (1976) seeding; Moody chart equivalent.",
    notes = "Valid for circular pipes. ε/D is computed from your inputs; ε = 0 gives a hydraulically smooth pipe.",
    keywords = listOf("friction factor", "colebrook", "moody", "roughness", "darcy"),
    inputs = listOf(
        InputSpec("re", "Reynolds number", "Re", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("eps", "Absolute roughness", "ε", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "mm", libraryKey = "roughness"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object FrictionFactorCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val re = value(inputs, "re")
        val d = value(inputs, "d")
        val relRough = if (has(inputs, "eps")) value(inputs, "eps") / d else 0.0

        if (relRough >= 0.1) {
            throw ValidationException(
                listOf(InputError("eps", "Relative roughness ε/D = ${Fmt.n(relRough, 4)} is outside the valid range for this correlation (must be < 0.1)."))
            )
        }

        val f = FrictionFactor.darcy(re, relRough)
        val regime = FrictionFactor.regimeName(re)

        return CalcOutput(
            results = listOf(
                result("f", "Friction Factor", f, "dash", isPrimary = true),
                result("reld", "Relative Roughness ε/D", relRough, "dash"),
            ),
            steps = listOf(
                "Relative roughness: ε/D = ${Fmt.n(if (has(inputs, "eps")) value(inputs, "eps") else 0.0, 6)} / ${Fmt.n(d, 4)} = ${Fmt.n(relRough, 6)}",
                if (re < FrictionFactor.LAMINAR_LIMIT) {
                    "Laminar flow (Re < 2300): f = 64/Re = 64 / ${Fmt.n(re, 1)} = ${Fmt.n(f, 5)}"
                } else {
                    "Turbulent: Colebrook-White solved iteratively (tolerance 1e-12) → f = ${Fmt.n(f, 5)}"
                },
                "Flow regime: $regime",
            ),
            stepsAr = listOf(
                "الخشونة النسبية: ε/D = ${Fmt.n(if (has(inputs, "eps")) value(inputs, "eps") else 0.0, 6)} / ${Fmt.n(d, 4)} = ${Fmt.n(relRough, 6)}",
                if (re < FrictionFactor.LAMINAR_LIMIT) {
                    "سريان صفحي (Re < 2300): f = 64/Re = 64 / ${Fmt.n(re, 1)} = ${Fmt.n(f, 5)}"
                } else {
                    "سريان مضطرب: كولبروك-وايت بحل تكراري (تسامح 1e-12) ← f = ${Fmt.n(f, 5)}"
                },
                "نظام السريان: " + when {
                    re < FrictionFactor.LAMINAR_LIMIT -> "صفحي"
                    re <= 4000.0 -> "انتقالي"
                    else -> "مضطرب"
                },
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
