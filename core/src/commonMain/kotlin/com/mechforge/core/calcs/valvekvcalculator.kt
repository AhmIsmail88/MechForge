package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.sqrt

/** Valve flow coefficient (liquid sizing): Q = Kv·√(ΔP/SG). */
private val Def = CalculatorDefinition(
    id = "valve-kv",
    name = "Valve Flow Coefficient (Kv / Cv)",
    category = CalculatorCategory.PIPING,
    description = "Liquid flow through a valve from its Kv, pressure drop and specific gravity; Cv reported for reference.",
    formulaDisplay = "Q = Fp*Kv*sqrt(min(dP,dP_choked)[bar]/SG); dP_choked = (FLP/Fp)^2*(P1-FF*Pv); FF=0.96-0.28*sqrt(Pv/Pc)",
    reference = "IEC 60534-2-1 liquid-sizing form; Emerson Control Valve Handbook, chapter 5: FF, Fp, FLP and limiting pressure drop. https://www.emerson.com/en/final-control/catalog/products-and-software/valves/control-valves/control-valve-handbook",
    notes = "Turbulent liquid service. Enter P1, Pv, Pc and FL together for a choked-flow limit; all pressures are absolute except dP. Without attached fittings Fp=1 and FLP=FL. If fittings are present, enter both manufacturer Fp and FLP for the same valve opening. Incipient cavitation, noise, erosion, viscosity and gas/steam sizing remain separate checks.",
    keywords = listOf("valve", "kv", "cv", "flow coefficient", "control valve", "sizing"),
    inputs = listOf(
        InputSpec("kv", "Flow coefficient", "Kv", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("dp", "Pressure drop across valve", "ΔP", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("sg", "Specific gravity (water = 1)", "SG", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("p1", "Absolute upstream pressure", "P1", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("pv", "Absolute vapour pressure at operating temperature", "Pv", UnitFamily.PRESSURE, required = false, minValue = 0.0, defaultUnitId = "bar"),
        InputSpec("pc", "Absolute thermodynamic critical pressure", "Pc", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("fl", "Manufacturer liquid pressure recovery factor", "FL", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "dash"),
        InputSpec("fp", "Manufacturer piping geometry factor", "Fp", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "dash"),
        InputSpec("flp", "Manufacturer combined recovery factor with fittings", "FLP", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "dash"),
    ),
)

object ValveKvCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val kv = value(inputs, "kv")
        val dpPa = value(inputs, "dp")
        val sg = optionalValue(inputs, "sg", 1.0)

        val dpBar = dpPa / 1e5
        val keys = listOf("p1", "pv", "pc", "fl")
        val detailed = keys.all { has(inputs, it) }
        fun invalid(id: String, message: String): Nothing = throw com.mechforge.core.engine.ValidationException(
            listOf(com.mechforge.core.engine.InputError(id, message)))
        if (keys.any { has(inputs, it) } && !detailed) invalid("p1", "Provide P1, Pv, Pc and FL together for liquid choking assessment.")
        if (has(inputs, "fp") != has(inputs, "flp")) invalid("fp", "Provide both Fp and FLP for attached fittings.")
        if (has(inputs, "fp") && !detailed) invalid("p1", "Fitting factors require the complete pressure and recovery-factor data.")
        val fp = optionalValue(inputs, "fp", 1.0)
        var ff = 0.0
        var limitBar: Double? = null
        var flashing = false
        if (detailed) {
            val p1 = value(inputs, "p1")
            val pv = value(inputs, "pv")
            val pc = value(inputs, "pc")
            val flp = optionalValue(inputs, "flp", value(inputs, "fl"))
            if (pv >= pc) invalid("pv", "Vapour pressure must be below thermodynamic critical pressure for this liquid model.")
            if (p1 <= pv) invalid("p1", "Upstream pressure must exceed vapour pressure for single-phase liquid inlet sizing.")
            if (dpPa >= p1) invalid("dp", "Pressure drop must leave a positive absolute downstream pressure.")
            if (flp > fp) invalid("flp", "FLP/Fp must not exceed 1 for this supported liquid model.")
            ff = 0.96 - 0.28 * sqrt(pv / pc)
            limitBar = (flp / fp) * (flp / fp) * (p1 - ff * pv) / 1e5
            flashing = p1 - dpPa <= pv
        }
        val sizingDrop = minOf(dpBar, limitBar ?: dpBar)
        val qM3h = fp * kv * sqrt(sizingDrop / sg)
        val cv = 1.156 * kv

        return CalcOutput(
            results = listOf(
                result("q", "Preliminary Liquid Flow Rate", qM3h, "m3h", isPrimary = true),
                result("cv", "Equivalent Cv (US)", cv, "dash"),
                result("dp", "Pressure Drop", dpBar, "bar"),
            ) + if (limitBar != null) listOf(
                result("dpChoked", "Choked pressure-drop limit", limitBar, "bar"),
                result("dpSizing", "Pressure drop used for sizing", sizingDrop, "bar"),
                result("ff", "Liquid critical pressure ratio factor", ff, "dash"),
            ) else emptyList(),
            steps = listOf(
                "Pressure drop: ΔP = ${Fmt.n(dpPa, 1)} Pa = ${Fmt.n(dpBar, 3)} bar",
                "Flow: Q = Fp*Kv*sqrt(dP_sizing/SG) = ${Fmt.n(fp, 3)} * ${Fmt.n(kv, 3)} * sqrt(${Fmt.n(sizingDrop, 3)}/${Fmt.n(sg, 3)}) = ${Fmt.n(qM3h, 3)} m³/h",
                "Equivalent Cv = 1.156 × Kv = ${Fmt.n(cv, 3)}",
                if (limitBar != null) "FF = ${Fmt.n(ff, 5)}; dP_choked = (FLP/Fp)^2*(P1-FF*Pv) = ${Fmt.n(limitBar, 5)} bar; dP_sizing = min(dP,dP_choked) = ${Fmt.n(sizingDrop, 5)} bar"
                else "No choking calculation: pressure and recovery data were not supplied.",
            ),
            warnings = buildList {
                if (!detailed) add("Choking is unverified: enter P1, Pv, Pc and FL. The basic result assumes turbulent, non-choked liquid flow without attached fittings.")
                else {
                    if (dpBar >= limitBar!!) add("Flow is choked; capacity uses the limiting pressure drop. This does not establish acceptable cavitation, erosion or noise.")
                    if (flashing) add("Downstream pressure is at or below vapour pressure: flashing service requires a suitable valve and manufacturer review.")
                    if (!has(inputs, "fp")) add("No attached fittings assumed: Fp=1 and FLP=FL; provide both factors if reducers or fittings are present.")
                }
                if (!has(inputs, "sg")) add("Specific gravity not provided — assumed 1.0 (water).")
                add("Liquid sizing only; cavitation onset/damage, noise, laminar flow and gas/steam sizing are not validated.")
            },
            stepsAr = listOf(
                "فرق الضغط: ΔP = ${Fmt.n(dpPa, 1)} Pa = ${Fmt.n(dpBar, 3)} bar",
                "التدفق: Q = Fp*Kv*sqrt(dP_sizing/SG) = ${Fmt.n(fp, 3)} * ${Fmt.n(kv, 3)} * sqrt(${Fmt.n(sizingDrop, 3)}/${Fmt.n(sg, 3)}) = ${Fmt.n(qM3h, 3)} m³/h",
                "المكافئ Cv = 1.156 × Kv = ${Fmt.n(cv, 3)}",
                if (limitBar != null) "FF = ${Fmt.n(ff, 5)}; dP_choked = (FLP/Fp)^2*(P1-FF*Pv) = ${Fmt.n(limitBar, 5)} bar; dP_sizing = min(dP,dP_choked) = ${Fmt.n(sizingDrop, 5)} bar"
                else "لم يُحسب الاختناق: بيانات الضغط والاستعادة غير مدخلة.",
            ),
            warningsAr = buildList {
                if (!detailed) add("الاختناق غير متحقق: أدخل P1 وPv وPc وFL. النتيجة الأساسية تفترض سائلًا مضطربًا غير مختنق دون وصلات ملحقة.")
                else {
                    if (dpBar >= limitBar!!) add("السريان مختنق؛ استُخدم فرق الضغط الحدي لحساب السعة. هذا لا يثبت قبول التكهف أو التآكل أو الضوضاء.")
                    if (flashing) add("ضغط المصب عند ضغط البخار أو أقل: خدمة التبخر تحتاج صمامًا ملائمًا ومراجعة المصنع.")
                    if (!has(inputs, "fp")) add("افتُرض عدم وجود وصلات ملحقة: Fp=1 وFLP=FL؛ أدخل المعاملين عند وجود مخفضات أو وصلات.")
                }
                if (!has(inputs, "sg")) {
                    add("لم تُدخل الكثافة النوعية - افتُرضت 1.0 (مياه).")
                }
                add("حساب السوائل فقط؛ بدء التكهف وأضراره والضوضاء والسريان الصفحي والغازات والبخار غير متحققة.")
            },
        )
    }
}
