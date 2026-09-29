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
 * Pump hydraulic power and shaft power + recommended standard IEC motor.
 * P_h = ρ·g·Q·H ; P = P_h / η
 */

private val Def = CalculatorDefinition(
    id = "pump-power",
    name = "Pump Hydraulic Power & Shaft Power",
    category = CalculatorCategory.HYDRAULICS,
    description = "Hydraulic (water) power, shaft power and recommended standard IEC motor for a pump duty point.",
    formulaDisplay = "P_h = ρ·g·Q·H ;  P = P_h / η",
    reference = "Pump hydraulics (Karassik et al., Pump Handbook); IEC 60034 standard motor ratings.",
    notes = "g = 9.80665 m/s². Density defaults to 1000 kg/m³, the round value for water - water at 20 °C is 998.2 kg/m³, a difference of under 0.2 %, and the fire-pump and NPSH calculators use that accurate figure. Enter the density for anything else. Efficiency is the pump (hydraulic-to-shaft) efficiency as a fraction.",
    keywords = listOf("pump", "power", "shaft", "hydraulic", "motor", "efficiency", "duty"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("h", "Total head", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec(
            "eta", "Pump efficiency", "η", UnitFamily.DIMENSIONLESS,
            minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct",
        ),
        InputSpec("rho", "Fluid density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density", assumedWhenOmitted = "Fluid density assumed as 1000 kg/m3 (water) - use the value for the fluid actually handled."),
    ),
)

object PumpPowerCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val h = value(inputs, "h") // m
        val eta = value(inputs, "eta") // fraction
        // The round 1000 kg/m³ rather than 998.2: the difference is under 0.2 % here, and this calculator is used for liquids in general, not only water at 20 C.
        val rho = optionalValue(inputs, "rho", 1000.0) // kg/m³

        val hydraulicW = rho * G * q * h
        val shaftW = hydraulicW / eta
        val hydraulicKw = hydraulicW / 1000.0
        val shaftKw = shaftW / 1000.0
        val motor = IEC_MOTOR_RATINGS_KW.firstOrNull { it * 1000.0 >= shaftW }

        val warnings = buildList {
            if (eta > 0.85) {
                add("Efficiency above 85% is optimistic for most pump types; verify against the pump curve.")
            }
            if (motor == null) {
                add("Shaft power is above the largest rating in the standard list (355 kW): no standard motor can be recommended here - select a larger or custom driver, and state the selection basis in the report.")
            }
        }

        return CalcOutput(
            results = buildList {
                add(result("hydraulic", "Hydraulic Power", hydraulicKw, "kw", isPrimary = true))
                add(result("shaft", "Shaft Power", shaftKw, "kw", isPrimary = true))
                motor?.let { r ->
                    add(result("motor", "Recommended Standard Motor", r, "kw", isRecommended = true))
                }
            },
            steps = listOf(
                "Hydraulic power: P_h = ρ·g·Q·H = ${Fmt.n(rho, 1)} × 9.80665 × ${Fmt.n(q, 6)} × ${Fmt.n(h, 3)} = ${Fmt.n(hydraulicKw)} kW",
                "Shaft power: P = P_h / η = ${Fmt.n(hydraulicKw)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw)} kW",
                "Select the smallest standard IEC motor rating ≥ shaft power → " +
                    (motor?.let { "${Fmt.n(it, 2)} kW" } ?: "beyond the standard list (355 kW) - custom driver selection"),
            ),
            warnings = warnings,
            stepsAr = listOf(
                "القدرة الهيدروليكية: P_h = ρ·g·Q·H = ${Fmt.n(rho, 1)} × 9.80665 × ${Fmt.n(q, 6)} × ${Fmt.n(h, 3)} = ${Fmt.n(hydraulicKw)} kW",
                "قدرة العمود: P = P_h / η = ${Fmt.n(hydraulicKw)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw)} kW",
                "اختر أصغر قدرة محرك قياسية IEC أكبر من أو تساوي قدرة العمود → " +
                    (motor?.let { "${Fmt.n(it, 2)} kW" } ?: "أعلى من قائمة القدرات القياسية (355 kW) - اختيار محرك خاص"),
            ),
            warningsAr = buildList {
                if (eta > 0.85) {
                    add("كفاءة أعلى من 85 % تفاؤلية لمعظم أنواع المضخات؛ تحقق منها على منحنى المضخة.")
                }
                if (motor == null) {
                    add("قدرة العمود أعلى من أكبر قدرة في القائمة القياسية (355 kW): لا يمكن ترشيح محرك قياسي - اختر محركًا أكبر أو خاصًا، واذكر أساس الاختيار في التقرير.")
                }
            },
        )
    }

}
