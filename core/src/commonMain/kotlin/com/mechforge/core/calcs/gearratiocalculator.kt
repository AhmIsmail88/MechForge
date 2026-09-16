package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Spur gear ratio, speed and torque relations. */
private val Def = CalculatorDefinition(
    id = "gear-ratio",
    name = "Gear Ratio / Speed / Torque",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Spur-gear ratio with output speed and torque, plus pitch diameters from the module.",
    formulaDisplay = "i = z₂/z₁ = n₁/n₂ ;  T₂ = T₁·i ;  d = m·z",
    reference = "Standard gear kinematics (e.g. Shigley, Mechanical Engineering Design).",
    notes = "Ideal (loss-free) torque relation. Real drives lose a few percent per mesh. Module m in mm; pitch diameter d = m·z.",
    keywords = listOf("gear", "ratio", "speed", "torque", "module", "pitch diameter"),
    inputs = listOf(
        InputSpec("z1", "Driving teeth", "z₁", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("z2", "Driven teeth", "z₂", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("n1", "Driving speed (optional)", "n₁", UnitFamily.ROTATIONAL_SPEED, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec("t1", "Driving torque (optional)", "T₁", UnitFamily.TORQUE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "nm"),
        InputSpec("m", "Module (optional)", "m", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object GearRatioCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val z1 = value(inputs, "z1")
        val z2 = value(inputs, "z2")

        val i = z2 / z1
        val results = mutableListOf(
            result("i", "Gear Ratio (z₂/z₁)", i, "dash", isPrimary = true),
        )
        val steps = mutableListOf(
            "Ratio: i = z₂/z₁ = ${Fmt.n(z2, 0)} / ${Fmt.n(z1, 0)} = ${Fmt.n(i, 4)}",
        )
        val stepsAr = mutableListOf(
            "النسبة: i = z₂/z₁ = ${Fmt.n(z2, 0)} / ${Fmt.n(z1, 0)} = ${Fmt.n(i, 4)}",
        )

        if (has(inputs, "n1")) {
            val n1 = value(inputs, "n1")
            val n2 = n1 / i
            results += result("n2", "Driven Speed", n2, "rpm", isPrimary = true)
            steps += "Output speed: n₂ = n₁/i = ${Fmt.n(n1, 1)} / ${Fmt.n(i, 4)} = ${Fmt.n(n2, 1)} rpm"
            stepsAr += "سرعة الخرج: n₂ = n₁/i = ${Fmt.n(n1, 1)} / ${Fmt.n(i, 4)} = ${Fmt.n(n2, 1)} rpm"
        }
        if (has(inputs, "t1")) {
            val t1 = value(inputs, "t1")
            val t2 = t1 * i
            results += result("t2", "Driven Torque (ideal)", t2, "nm", isPrimary = true)
            steps += "Output torque: T₂ = T₁·i = ${Fmt.n(t1, 2)} × ${Fmt.n(i, 4)} = ${Fmt.n(t2, 2)} N·m (ideal, frictionless)"
            stepsAr += "عزم الخرج: T₂ = T₁·i = ${Fmt.n(t1, 2)} × ${Fmt.n(i, 4)} = ${Fmt.n(t2, 2)} N·m (مثالي بدون احتكاك)"
        }
        if (has(inputs, "m")) {
            val m = value(inputs, "m")
            val d1 = m * z1
            val d2 = m * z2
            results += result("d1", "Pitch Diameter (pinion)", d1 * 1000.0, "mm")
            results += result("d2", "Pitch Diameter (gear)", d2 * 1000.0, "mm")
            steps += "Pitch diameters: d₁ = m·z₁ = ${Fmt.n(d1 * 1000.0, 2)} mm ; d₂ = m·z₂ = ${Fmt.n(d2 * 1000.0, 2)} mm"
            stepsAr += "أقطار التقسيم: d₁ = m·z₁ = ${Fmt.n(d1 * 1000.0, 2)} mm ؛ d₂ = m·z₂ = ${Fmt.n(d2 * 1000.0, 2)} mm"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            stepsAr = stepsAr,
            warnings = if (i > 6.0) {
                listOf("Single-stage ratio above 6:1 — consider a two-stage arrangement for load capacity and size.")
            } else {
                emptyList()
            },
            warningsAr = if (i > 6.0) {
                listOf("نسبة مرحلة واحدة أعلى من 6:1 — فكّر في ترتيب على مرحلتين لقدرة الحمل والحجم.")
            } else {
                emptyList()
            },
        )
    }
}
