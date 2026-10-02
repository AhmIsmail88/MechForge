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
 * Pressure design thickness of a pipe, following the ASME B31.3 form of the hoop-stress
 * (Barlow) equation:
 *
 *     t = p*D / (2*(S*E*W + p*Y))
 *
 * S = allowable stress from the code material table, E = weld joint quality factor,
 * Y = coefficient from B31.3 Table 304.1.1. The corrosion/erosion allowance is added, and
 * the required NOMINAL thickness is then obtained by dividing out the mill tolerance
 * (12.5% is a common ASTM A106/A53 pipe specification tolerance, not a B31.3 constant).
 */
private val Def = CalculatorDefinition(
    id = "pipe-wall-thickness",
    name = "Pipe Wall Thickness (ASME B31.3 Style)",
    category = CalculatorCategory.PIPING,
    description = "Pressure design thickness for pipe or tube using the B31.3 equation, including the weld joint factor, the Y coefficient, the corrosion allowance and the mill tolerance.",
    formulaDisplay = "t = p*D / (2*(S*E*W + p*Y)) + CA + MA   then   t_nom = t / (1 - mill tolerance)",
    reference = "ASME B31.3 para. 304.1.2 pressure design thickness t = PD/(2(SEW + PY)); obtain the actual mill tolerance from the specified pipe product standard (12.5% is common for ASTM A106/A53, not a B31.3 constant).",
    notes = "S is the code allowable stress at the DESIGN TEMPERATURE (not yield). E is the weld joint quality factor (1.00 seamless, values for the actual weld type and examination from the code tables). Y comes from B31.3 Table 304.1.1 (0.4 for ferritic steels up to 482 C). The nominal wall thickness must satisfy t_min PLUS the mill tolerance, so the required nominal thickness is t_min/(1 - mill tolerance) - order the next heavier standard wall thickness. Structural loads, supports, hydrotest and the pipe standard's schedule must also be checked.",
    keywords = listOf("wall thickness", "b31.3", "b31.1", "pressure design", "pipe", "schedule", "hoop stress", "mill tolerance", "joint factor"),
    inputs = listOf(
        InputSpec("p", "Design pressure", "p", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("d", "Outside diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm", libraryKey = "pipe-od"),
        InputSpec("sigma", "Allowable stress S (at design temperature)", "S", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("weld", "Weld joint strength reduction factor W", "W", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "dash", defaultValue = 1.0,
            assumedWhenOmitted = "W = 1.0 assumed; verify the weld strength reduction factor for the material and design temperature under ASME B31.3."),
        InputSpec("ma", "Mechanical allowance (threads, grooves, machining)", "MA", UnitFamily.LENGTH,
            required = false, minValue = 0.0, defaultUnitId = "mm", defaultValue = 0.0,
            assumedWhenOmitted = "No mechanical allowance assumed; add the required thread, groove or machining allowance."),
        InputSpec(
            "e", "Weld joint quality factor E", "E", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0,
            defaultUnitId = "dash", defaultValue = 1.0,
            assumedWhenOmitted = "Weld joint quality factor E assumed as 1.00 (seamless pipe). Obtain E for the specified joint and examination from the code tables.",
        ),
        InputSpec(
            "y", "Coefficient Y (B31.3 Table 304.1.1)", "Y", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = false, maxValue = 1.0,
            defaultUnitId = "dash", defaultValue = 0.4,
            assumedWhenOmitted = "Coefficient Y assumed as 0.4, the B31.3 Table 304.1.1 value for ferritic steels up to 482 C - other materials and temperatures differ.",
        ),
        InputSpec(
            "ca", "Corrosion / erosion allowance", "CA", UnitFamily.LENGTH,
            required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "mm", defaultValue = 0.0,
            assumedWhenOmitted = "No corrosion or erosion allowance assumed (0 mm) - codes require an allowance for the service; a clean, non-corrosive service may justify zero.",
        ),
        InputSpec(
            "mill", "Mill tolerance", "mill", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = false, maxValue = 1.0, exclusiveMax = true,
            allowedUnitIds = listOf("pct"), defaultUnitId = "pct", defaultValue = 12.5,
            assumedWhenOmitted = "Mill tolerance assumed as 12.5 %, the standard allowance for seamless and welded pipe under the ASTM A106/A53 specifications - it is a pipe specification figure, not a B31.3 one. Confirm the tolerance for the pipe actually ordered.",
        ),
    ),
)

object PipeWallThicknessCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p") // Pa
        val d = value(inputs, "d") // m
        val s = value(inputs, "sigma") // Pa
        val e = optionalValue(inputs, "e", 1.0)
        val weld = optionalValue(inputs, "weld", 1.0)
        val mechanical = optionalValue(inputs, "ma", 0.0)
        val y = optionalValue(inputs, "y", 0.4)
        val ca = optionalValue(inputs, "ca", 0.0) // m
        val mill = optionalValue(inputs, "mill", 0.125) // fraction

        val tPressure = p * d / (2.0 * (s * e * weld + p * y))
        if (tPressure >= d / 6.0 || p / (s * e) > 0.385) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("p", "Outside the simple B31.3 pressure equation limits: t must be < D/6 and P/(S*E) <= 0.385. Special pressure design analysis is required.")))
        }
        val tMin = tPressure + ca + mechanical
        val tNominal = tMin / (1.0 - mill)
        if (tNominal >= d / 2.0) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("ca", "The allowances and tolerance leave no positive bore; review the dimensions.")))
        }

        val warnings = buildList {
            // B31.3 304.1.2(a): the pressure-design equation holds while t < D/6. Beyond that the
            // geometry is no longer thin-walled and the hoop-stress form stops being valid.
            if (d / tNominal < 6.0) add("The nominal wall including allowances is thick relative to the outside diameter; verify the selected product geometry.")
            add("S must be the code allowable stress at the design temperature, and E/Y must match the pipe and material - confirm against ASME B31.3 (or the governing code) before ordering.")
            if (e < 1.0) add("E = ${Fmt.n(e, 3)}: the allowable is reduced for a welded joint - confirm the joint factor for the examination level specified.")
            if (ca <= 0.0) add("No corrosion/erosion allowance entered - codes require an allowance for the service (0 for clean non-corrosive service).")
            add("The nominal thickness must also cover the mill tolerance and any thinning (bending, threading/grooving); the next heavier standard wall thickness is normally selected.")
        }

        return CalcOutput(
            results = listOf(
                result("tp", "Pressure Design Thickness t", tPressure * 1000.0, "mm"),
                result("t", "Minimum Thickness (t + CA + MA)", tMin * 1000.0, "mm", isPrimary = true),
                result("tNom", "Required Nominal Thickness (incl. mill tolerance)", tNominal * 1000.0, "mm", isPrimary = true),
                result("odRatio", "Nominal D/t Ratio", d / tNominal, "dash"),
            ),
            steps = listOf(
                "p = ${Fmt.n(p / 1e5, 4)} bar   D = ${Fmt.n(d * 1000.0, 2)} mm   S = ${Fmt.n(s / 1e6, 1)} MPa   E = ${Fmt.n(e, 3)}   W = ${Fmt.n(weld, 3)}   Y = ${Fmt.n(y, 3)}",
                "t = p*D/(2*(S*E*W + p*Y)) = ${Fmt.n(p, 1)} x ${Fmt.n(d, 5)} / (2 x (${Fmt.n(s, 1)} x ${Fmt.n(e, 3)} x ${Fmt.n(weld, 3)} + ${Fmt.n(p, 1)} x ${Fmt.n(y, 3)})) = ${Fmt.n(tPressure * 1000.0, 4)} mm",
                "Minimum thickness: t + CA + MA = ${Fmt.n(tPressure * 1000.0, 4)} + ${Fmt.n(ca * 1000.0, 3)} + ${Fmt.n(mechanical * 1000.0, 3)} = ${Fmt.n(tMin * 1000.0, 4)} mm",
                "Required nominal thickness: ${Fmt.n(tMin * 1000.0, 4)} / (1 - ${Fmt.n(mill * 100.0, 2)} %) = ${Fmt.n(tNominal * 1000.0, 4)} mm",
                "Order the next heavier standard wall thickness (select by schedule and verify against the pipe standard).",
            ),
            stepsAr = listOf(
                "p = ${Fmt.n(p / 1e5, 4)} bar   D = ${Fmt.n(d * 1000.0, 2)} mm   S = ${Fmt.n(s / 1e6, 1)} MPa   E = ${Fmt.n(e, 3)}   W = ${Fmt.n(weld, 3)}   Y = ${Fmt.n(y, 3)}",
                "t = p·D/(2·(S·E·W + p·Y)) = ${Fmt.n(p, 1)} × ${Fmt.n(d, 5)} / (2 × (${Fmt.n(s, 1)} × ${Fmt.n(e, 3)} × ${Fmt.n(weld, 3)} + ${Fmt.n(p, 1)} × ${Fmt.n(y, 3)})) = ${Fmt.n(tPressure * 1000.0, 4)} mm",
                "السماكة الدنيا: t + CA + MA = ${Fmt.n(tPressure * 1000.0, 4)} + ${Fmt.n(ca * 1000.0, 3)} + ${Fmt.n(mechanical * 1000.0, 3)} = ${Fmt.n(tMin * 1000.0, 4)} mm",
                "السماكة الاسمية المطلوبة: ${Fmt.n(tMin * 1000.0, 4)} / (1 − ${Fmt.n(mill * 100.0, 2)} %) = ${Fmt.n(tNominal * 1000.0, 4)} mm",
                "اطلب السماكة القياسية الأثقل التالية (حسب الـ schedule، وراجع مقابل معيار المواسير).",
            ),
            warnings = warnings,
            warningsAr = buildList {
                if (d / tNominal < 6.0) add("السماكة الاسمية مع البدلات كبيرة بالنسبة للقطر الخارجي؛ تحقق من هندسة القطاع المختار.")
                add("S يجب أن تكون الإجهاد المسموح وفق الكود عند حرارة التصميم، وE و Y مطابقين للماسورة والمادة - أكّدها مقابل ASME B31.3 (أو الكود الحاكم) قبل الطلب.")
                if (e < 1.0) {
                    add("E = ${Fmt.n(e, 3)}: الإجهاد المسموح مخفّض لوصلة ملحومة - أكّد معامل الوصلة لمستوى الفحص المحدد.")
                }
                if (ca <= 0.0) {
                    add("لم يُدخل بدل التآكل - الأكواد تشترط بدلًا حسب الخدمة (صفر للخدمة النظيفة غير المسبِّبة للتآكل).")
                }
                add("السماكة الاسمية لازم تغطي تفاوت الطاحونة وأي ترقيق (الثني أو القلاووظ أو التجليف)؛ وعادة يُختار الجدول الأثقل التالي.")
            },
        )
    }
}
