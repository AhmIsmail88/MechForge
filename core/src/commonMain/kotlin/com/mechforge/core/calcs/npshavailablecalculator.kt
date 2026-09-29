package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** NPSH available at the pump suction. */
private val Def = CalculatorDefinition(
    id = "npsh-available",
    name = "NPSH Available",
    category = CalculatorCategory.HYDRAULICS,
    description = "Net Positive Suction Head available at the pump inlet, from absolute pressure, vapour pressure, static lift and suction losses.",
    formulaDisplay = "NPSHa = (p_atm − p_v)/(ρ·g) + h_static − h_friction",
    reference = "Standard pump suction-head relation (e.g. Karassik, Pump Handbook; HI standards).",
    notes = "g = 9.80665 m/s². h_static may be negative (pump above the liquid surface). Compare NPSHa with the pump curve NPSHr — this tool does not know your pump.",
    keywords = listOf("npsh", "cavitation", "pump", "suction", "vapour pressure"),
    inputs = listOf(
        InputSpec("patm", "Absolute pressure at surface", "p_atm", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("alt", "Site altitude (computes the atmospheric pressure)", "alt", UnitFamily.LENGTH, required = false, defaultUnitId = "m"),
        InputSpec("npshr", "NPSH required by the pump (at the duty flow)", "NPSHr", UnitFamily.LENGTH, required = false, minValue = 0.0, defaultUnitId = "m"),
        InputSpec("pv", "Vapour pressure of liquid", "p_v", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "kpa"),
        InputSpec("rho", "Liquid density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
        InputSpec("hs", "Static suction head", "h_static", UnitFamily.LENGTH, defaultUnitId = "m"),
        InputSpec("hf", "Suction line losses", "h_friction", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = false, defaultUnitId = "m"),
    ),
)

object NpshAvailableCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        // Atmospheric pressure: an explicit input wins, then the site altitude through the ISA
        // profile, then the sea-level shorthand. The default is material - at 600 m it is about
        // 94 kPa, which costs roughly 0.7 m of NPSHa.
        val pAtm = when {
            has(inputs, "patm") -> value(inputs, "patm")
            has(inputs, "alt") -> atmosphericPressure(value(inputs, "alt"))
            else -> 101325.0
        }
        val npshr = if (has(inputs, "npshr")) value(inputs, "npshr") else null
        val pV = optionalValue(inputs, "pv", 2339.0)
        val rho = optionalValue(inputs, "rho", 998.2)
        val hStatic = value(inputs, "hs")
        val hFriction = value(inputs, "hf")

        val pressureHead = (pAtm - pV) / (rho * G)
        val npsha = pressureHead + hStatic - hFriction

        val warnings = buildList {
            if (!has(inputs, "pv")) add("Vapour pressure not provided — assumed 2.339 kPa (water at 20 °C).")
            if (!has(inputs, "rho")) add("Density not provided — assumed 998.2 kg/m³ (water at 20 °C).")
            if (!has(inputs, "patm") && !has(inputs, "alt")) add("Atmospheric pressure not provided - assumed 101.325 kPa, the sea-level value. At altitude it is lower and NPSHa falls with it; enter the site altitude and it is computed.")
            npshr?.let { r ->
                val margin = npsha - r
                if (margin < 0.5) add("NPSH margin (NPSHa - NPSHr) is ${Fmt.n(margin, 3)} m - below the 0.5 m minimum margin of ANSI/HI 9.6.1.")
            }
            if (npsha < 3.0) add("NPSHa below 3 m - a common rule of thumb, not a code limit. Verify against the pump NPSHr curve with an adequate safety margin (ANSI/HI 9.6.1).")
            if (npsha <= 0.0) add("NPSHa is not positive — the pump would cavitate at this duty.")
        }

        return CalcOutput(
            results = buildList {
                add(result("npsha", "NPSH Available", npsha, "m", isPrimary = true))
                add(result("phead", "Pressure Head Contribution", pressureHead, "m"))
                npshr?.let { r ->
                    add(result("npshr", "NPSH Required (pump curve)", r, "m"))
                    add(result("margin", "NPSH margin (NPSHa - NPSHr)", npsha - r, "m"))
                    add(result("marginRatio", "NPSHa / NPSHr", npsha / r, "dash"))
                }
            },
            steps = listOf(
                "Pressure head: (p_atm − p_v)/(ρ·g) = (${Fmt.n(pAtm, 0)} − ${Fmt.n(pV, 0)}) / (${Fmt.n(rho, 1)} × 9.80665) = ${Fmt.n(pressureHead, 3)} m",
                "Static head: ${Fmt.n(hStatic, 3)} m   Suction losses: −${Fmt.n(hFriction, 3)} m",
                "NPSHa = ${Fmt.n(pressureHead, 3)} + ${Fmt.n(hStatic, 3)} − ${Fmt.n(hFriction, 3)} = ${Fmt.n(npsha, 3)} m",
            ),
            warnings = warnings,
            stepsAr = listOf(
                "مساهمة ضغط الرأس: (p_atm − p_v)/(ρ·g) = (${Fmt.n(pAtm, 0)} − ${Fmt.n(pV, 0)}) / (${Fmt.n(rho, 1)} × 9.80665) = ${Fmt.n(pressureHead, 3)} m",
                "الرفع الاستاتيكي: ${Fmt.n(hStatic, 3)} m   فقد خط الشفط: −${Fmt.n(hFriction, 3)} m",
                "NPSHa = ${Fmt.n(pressureHead, 3)} + ${Fmt.n(hStatic, 3)} − ${Fmt.n(hFriction, 3)} = ${Fmt.n(npsha, 3)} m",
            ),
            warningsAr = buildList {
                if (!has(inputs, "pv")) {
                    add("لم يُدخل ضغط البخار - افتُرض 2.339 kPa (مياه عند 20 °C).")
                }
                if (!has(inputs, "rho")) {
                    add("لم تُدخل الكثافة - افتُرضت 998.2 kg/m³ (مياه عند 20 °C).")
                }
                if (!has(inputs, "patm") && !has(inputs, "alt")) {
                    add("لم يُدخل الضغط الجوي - افتُرض 101.325 kPa (مستوى سطح البحر). عند الارتفاع يقل الضغط ويقل NPSHa معه؛ أدخل ارتفاع الموقع ويُحسب.")
                }
                npshr?.let { r ->
                    val margin = npsha - r
                    if (margin < 0.5) {
                        add("هامش NPSH (NPSHa - NPSHr) = ${Fmt.n(margin, 3)} m - أقل من الحد الأدنى 0.5 m وفق ANSI/HI 9.6.1.")
                    }
                }
                if (npsha < 3.0) {
                    add("NPSHa أقل من 3 m - قاعدة عملية شائعة وليست حد كود. تحقق مقابل منحنى NPSHr للمضخة بهامش أمان كافٍ (ANSI/HI 9.6.1).")
                }
                if (npsha <= 0.0) {
                    add("NPSHa غير موجب - المضخة ستتكهّف عند نقطة التشغيل هذه.")
                }
            },
        )
    }
}
