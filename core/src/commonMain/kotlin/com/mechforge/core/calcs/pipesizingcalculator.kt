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
import kotlin.math.sqrt

/** Velocity-based pipe sizing: D = √(4·Q/(π·v)). */

private val Def = CalculatorDefinition(
    id = "pipe-sizing",
    name = "Pipe Sizing (Velocity-Based)",
    category = CalculatorCategory.PIPING,
    description = "Theoretical internal diameter from flow rate and a chosen design velocity.",
    formulaDisplay = "D = √(4·Q / (π·v))",
    reference = "Continuity equation; velocity limits are engineering practice, not code.",
    notes = "Choose the velocity by line role, and keep suction lower than discharge: pump suction is commonly 0.6-1.8 m/s (the lower the better for NPSH margin), discharge 1.5-3.0 m/s as a starting point. Fire-pump piping has its own velocity limits under NFPA 20, and slurries or abrasive services need their own limits.",
    keywords = listOf("pipe", "sizing", "diameter", "velocity", "design"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("v", "Design velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
    ),
)

object PipeSizingCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val v = value(inputs, "v") // m/s

        val d = sqrt(4.0 * q / (PI * v)) // m
        val dMm = d * 1000.0

        val warnings = buildList {
            if (v > 3.0) add("Selected velocity is above 3 m/s: typical water practice keeps the suction at 0.6-1.8 m/s and the discharge at 1.5-3.0 m/s, and abrasive or slurry services are lower still.")
            if (v < 0.6) add("Selected velocity below 0.6 m/s — sedimentation and undersizing risk in water lines.")
            add("Result is the theoretical internal diameter; select the next standard pipe size (schedule/series) above it.")
        }

        return CalcOutput(
            results = listOf(
                result("d", "Required Internal Diameter", dMm, "mm", isPrimary = true),
                result("area", "Flow Area", PI * d * d / 4.0, "m2", isPrimary = false),
            ),
            steps = listOf(
                "D = √(4·Q/(π·v)) = √(4 × ${Fmt.n(q, 6)} / (π × ${Fmt.n(v, 3)})) = ${Fmt.n(d, 5)} m = ${Fmt.n(dMm, 1)} mm",
                "Flow area: A = π·D²/4 = ${Fmt.n(PI * d * d / 4.0, 6)} m²",
            ),
            warnings = warnings,
            stepsAr = listOf(
                "D = √(4·Q/(π·v)) = √(4 × ${Fmt.n(q, 6)} / (π × ${Fmt.n(v, 3)})) = ${Fmt.n(d, 5)} m = ${Fmt.n(dMm, 1)} mm",
                "مساحة السريان: A = π·D²/4 = ${Fmt.n(PI * d * d / 4.0, 6)} m²",
            ),
            warningsAr = buildList {
                if (v > 3.0) {
                    add("سرعة مختارة أعلى من 3 m/s - الممارسة المعتادة للمياه بين 0.6 و 3.0 m/s حسب دور الخط.")
                }
                if (v < 0.6) {
                    add("سرعة مختارة أقل من 0.6 m/s - خطر ترسيب ونقص في المقاس في خطوط المياه.")
                }
                add("الناتج هو القطر الداخلي النظري؛ اختر مقاس الماسورة القياسي التالي (الجدول أو السلسلة) الأكبر منه.")
            },
        )
    }

}
