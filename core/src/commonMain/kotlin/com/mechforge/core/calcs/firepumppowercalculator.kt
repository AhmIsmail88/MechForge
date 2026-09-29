package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Fire pump power: hydraulic power, shaft power and a standard IEC driver rating. */
private val Def = CalculatorDefinition(
    id = "fire-pump-power",
    name = "Fire Pump Power",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Hydraulic and shaft power for a fire pump duty point, with the next standard IEC motor rating.",
    formulaDisplay = "P_h = rho*g*Q*H ;  P_shaft = P_h/eta",
    reference = "Pump power relations; IEC 60034 standard motor ratings (NFPA 20 governs the actual driver selection).",
    notes = "eta is the pump efficiency at the duty point (from the certified curve). NFPA 20 requires the driver to carry the pump at its overload point, so the selected rating is normally above the shaft power - verify against the standard.",
    keywords = listOf("fire pump", "power", "motor", "diesel", "fire", "nfpa 20"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "lmin"),
        InputSpec("h", "Total head", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("eta", "Pump efficiency", "eta", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct"),
        InputSpec("rho", "Water density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
        InputSpec(
            "bhp150", "Brake power at 150 % of rated flow (certified curve)", "P_150", UnitFamily.POWER,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw",
            assumedWhenOmitted = "Driver sized from the shaft power at the duty point only: NFPA 20 requires the driver to carry the pump at 150 % of rated flow, so enter the brake power from the certified curve at that point.",
        ),
    ),
)

object FirePumpPowerCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val h = value(inputs, "h")
        val eta = value(inputs, "eta")
        val rho = optionalValue(inputs, "rho", 998.2)

        val hydraulicKw = rho * G * q * h / 1000.0
        val shaftKw = hydraulicKw / eta
        val bhp150 = if (has(inputs, "bhp150")) value(inputs, "bhp150") / 1000.0 else null
        val driverKw = maxOf(shaftKw, bhp150 ?: 0.0)
        val motor = IEC_MOTOR_RATINGS_KW.firstOrNull { it >= driverKw }

        return CalcOutput(
            results = buildList {
                add(result("hydraulic", "Hydraulic Power", hydraulicKw, "kw"))
                add(result("shaft", "Shaft Power", shaftKw, "kw", isPrimary = true))
                add(result("driver", "Governing driver power (duty / 150 % point)", driverKw, "kw"))
                motor?.let { r ->
                    add(result("motor", "Recommended Driver Rating", r, "kw", isRecommended = true))
                }
            },
            steps = listOf(
                "Flow: ${Fmt.n(q * 60000.0, 1)} L/min = ${Fmt.n(q, 5)} m3/s",
                "Hydraulic power: P_h = rho*g*Q*H = ${Fmt.n(rho, 1)} x 9.80665 x ${Fmt.n(q, 5)} x ${Fmt.n(h, 2)} = ${Fmt.n(hydraulicKw, 2)} kW",
                "Shaft power: P = P_h/eta = ${Fmt.n(hydraulicKw, 2)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw, 2)} kW",
                "Governing driver power: max(shaft ${Fmt.n(shaftKw, 2)} kW, 150 % point ${Fmt.n(bhp150 ?: shaftKw, 2)} kW) = ${Fmt.n(driverKw, 2)} kW",
                "Next standard driver rating: ${motor?.let { Fmt.n(it, 1) } ?: "beyond the list (355 kW)"} kW",
            ),
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 998.2 kg/m3 (water at 20 C).")
                if (!has(inputs, "bhp150")) {
                    add("No brake power entered for 150 % of rated flow: NFPA 20 sizes the driver on that point, not on the duty point - get it from the certified curve.")
                }
                if (motor == null) add("The governing driver power is above the largest rating in the standard list (355 kW) - select a larger or custom driver.")
                add("NFPA 20 requires the driver to be rated for the pump overload point - confirm before ordering.")
            },
            stepsAr = listOf(
                "التدفق: ${Fmt.n(q * 60000.0, 1)} L/min = ${Fmt.n(q, 5)} m3/s",
                "القدرة الهيدروليكية: P_h = rho*g*Q*H = ${Fmt.n(rho, 1)} × 9.80665 × ${Fmt.n(q, 5)} × ${Fmt.n(h, 2)} = ${Fmt.n(hydraulicKw, 2)} kW",
                "قدرة العمود: P = P_h/eta = ${Fmt.n(hydraulicKw, 2)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw, 2)} kW",
                "قدرة المحرك الحاكمة: max(قدرة العمود ${Fmt.n(shaftKw, 2)} kW، نقطة 150 % ${Fmt.n(bhp150 ?: shaftKw, 2)} kW) = ${Fmt.n(driverKw, 2)} kW",
                "أقرب قدرة قياسية: ${motor?.let { Fmt.n(it, 1) } ?: "أعلى من القائمة (355 kW)"} kW",
            ),
            warningsAr = buildList {
                if (!has(inputs, "rho")) {
                    add("لم تُدخل الكثافة - افتُرضت 998.2 kg/m3 (مياه عند 20 °C).")
                }
                if (!has(inputs, "bhp150")) {
                    add("لم تُدخل قدرة الفرملة عند 150 % من التصرف المقنن: NFPA 20 يقنّن المحرك على هذه النقطة لا على نقطة التشغيل - خذها من المنحنى المعتمد.")
                }
                if (motor == null) {
                    add("قدرة المحرك الحاكمة أعلى من أكبر قدرة في القائمة القياسية (355 kW) - اختر محركًا أكبر أو خاصًا.")
                }
                add("NFPA 20 يشترط أن يكون المحرك مُقنَّنًا عند نقطة الحمل الزائد للمضخة - تأكد قبل الشراء.")
            },
        )
    }
}
