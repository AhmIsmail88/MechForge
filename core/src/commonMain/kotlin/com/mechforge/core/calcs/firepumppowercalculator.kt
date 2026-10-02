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
    notes = "eta is the efficiency at the duty point. Driver selection requires the maximum brake power across the entire certified pump curve. The 150% flow point alone does not establish that maximum. The listed motor rating is preliminary; verify driver/controller ratings and site derating under NFPA 20.",
    keywords = listOf("fire pump", "power", "motor", "diesel", "fire", "nfpa 20"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "lmin"),
        InputSpec("h", "Total head", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("eta", "Pump efficiency", "eta", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct"),
        InputSpec("rho", "Water density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
        InputSpec(
            "bhp150", "Brake power at 150 % of rated flow (certified curve)", "P_150", UnitFamily.POWER,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw",
        ),
        InputSpec("bhpmax", "Maximum brake power across the entire certified pump curve", "P_max", UnitFamily.POWER,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
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
        val curveMax = inputs["bhpmax"]?.baseValue?.div(1000.0)
        if (curveMax != null && curveMax + 1e-9 < driverKw) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("bhpmax", "Maximum curve brake power cannot be below the duty or entered 150% point power.")))
        }
        val motor = curveMax?.let { maximum -> IEC_MOTOR_RATINGS_KW.firstOrNull { it >= maximum } }

        return CalcOutput(
            results = buildList {
                add(result("hydraulic", "Hydraulic Power", hydraulicKw, "kw"))
                add(result("shaft", "Shaft Power", shaftKw, "kw", isPrimary = true))
                curveMax?.let { add(result("driver", "Maximum certified curve brake power", it, "kw")) }
                motor?.let { r ->
                    add(result("motor", "Preliminary motor rating (verify NFPA 20 driver selection)", r, "kw"))
                }
            },
            steps = listOf(
                "Flow: ${Fmt.n(q * 60000.0, 1)} L/min = ${Fmt.n(q, 5)} m3/s",
                "Hydraulic power: P_h = rho*g*Q*H = ${Fmt.n(rho, 1)} x 9.80665 x ${Fmt.n(q, 5)} x ${Fmt.n(h, 2)} = ${Fmt.n(hydraulicKw, 2)} kW",
                "Shaft power: P = P_h/eta = ${Fmt.n(hydraulicKw, 2)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw, 2)} kW",
                "Maximum brake power across certified curve: ${curveMax?.let { Fmt.n(it, 2) + " kW" } ?: "not supplied; no motor selection"}",
                "Preliminary standard rating: ${motor?.let { Fmt.n(it, 1) + " kW" } ?: "not available"}",
            ),
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 998.2 kg/m3 (water at 20 C).")
                if (curveMax == null) add("No maximum brake power across the entire certified curve was entered; duty or 150% power alone cannot establish the driver rating.")
                if (curveMax != null && motor == null) add("Maximum brake power exceeds the standard rating list (355 kW).")
                add("Verify non-overloading over the entire pump curve, driver/controller suitability and ambient/altitude derating under NFPA 20 before selection.")
            },
            stepsAr = listOf(
                "التدفق: ${Fmt.n(q * 60000.0, 1)} L/min = ${Fmt.n(q, 5)} m3/s",
                "القدرة الهيدروليكية: P_h = rho*g*Q*H = ${Fmt.n(rho, 1)} × 9.80665 × ${Fmt.n(q, 5)} × ${Fmt.n(h, 2)} = ${Fmt.n(hydraulicKw, 2)} kW",
                "قدرة العمود: P = P_h/eta = ${Fmt.n(hydraulicKw, 2)} / ${Fmt.n(eta, 4)} = ${Fmt.n(shaftKw, 2)} kW",
                "أكبر قدرة فرملة عبر المنحنى المعتمد: ${curveMax?.let { Fmt.n(it, 2) + " kW" } ?: "غير مدخلة؛ لم يتم اختيار محرك"}",
                "قدرة قياسية أولية: ${motor?.let { Fmt.n(it, 1) + " kW" } ?: "غير متاحة"}",
            ),
            warningsAr = buildList {
                if (!has(inputs, "rho")) {
                    add("لم تُدخل الكثافة - افتُرضت 998.2 kg/m3 (مياه عند 20 °C).")
                }
                if (curveMax == null) add("لم تُدخل أكبر قدرة فرملة عبر كامل المنحنى المعتمد؛ نقطة التشغيل أو نقطة 150% وحدها لا تكفي لاختيار المحرك.")
                if (curveMax != null && motor == null) add("أكبر قدرة فرملة تتجاوز القائمة القياسية (355 kW).")
                add("تحقق من عدم زيادة الحمل عبر كامل منحنى المضخة وملاءمة المحرك والمتحكم وتصحيح الحرارة والارتفاع طبقًا لـ NFPA 20.")
            },
        )
    }
}
