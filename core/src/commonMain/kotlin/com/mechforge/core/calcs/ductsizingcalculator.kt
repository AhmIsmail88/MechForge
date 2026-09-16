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

/** Velocity-based duct sizing with an equivalent-round diameter. */
private val Def = CalculatorDefinition(
    id = "duct-sizing",
    name = "Duct Sizing (Velocity Method)",
    category = CalculatorCategory.HVAC,
    description = "Required duct cross-section from airflow and target velocity, with equivalent round diameter and rectangular sides.",
    formulaDisplay = "A = Q/v ;  D_eq = √(4A/π) ;  W = √(A·r), H = √(A/r)",
    reference = "Continuity equation; velocity method per standard HVAC design practice (ASHRAE-style).",
    notes = "r is the rectangular aspect ratio W/H (1.0 = square). Select the next standard duct size equal to or above the computed dimensions.",
    keywords = listOf("duct", "sizing", "velocity method", "equivalent diameter", "hvac"),
    inputs = listOf(
        InputSpec("q", "Airflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("v", "Target velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("r", "Aspect ratio W/H", "r", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object DuctSizingCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val v = value(inputs, "v")
        val ratio = optionalValue(inputs, "r", 1.0)

        val area = q / v
        val dEq = sqrt(4.0 * area / PI)
        val w = sqrt(area * ratio)
        val h = sqrt(area / ratio)

        return CalcOutput(
            results = listOf(
                result("a", "Required Duct Area", area, "m2", isPrimary = true),
                result("deq", "Equivalent Round Diameter", dEq * 1000.0, "mm", isPrimary = true),
                result("w", "Rectangular Width", w * 1000.0, "mm"),
                result("h", "Rectangular Height", h * 1000.0, "mm"),
            ),
            steps = listOf(
                "Area: A = Q/v = ${Fmt.n(q, 5)} / ${Fmt.n(v, 3)} = ${Fmt.n(area, 5)} m²",
                "Equivalent round: D_eq = √(4A/π) = ${Fmt.n(dEq, 4)} m = ${Fmt.n(dEq * 1000.0, 0)} mm",
                "Rectangular (W/H = ${Fmt.n(ratio, 2)}): W = ${Fmt.n(w * 1000.0, 0)} mm, H = ${Fmt.n(h * 1000.0, 0)} mm",
            ),
            warnings = buildList {
                add("Result is the theoretical minimum; select the next standard duct size equal to or above it.")
                if (v > 8.0) add("Target velocity above 8 m/s — check noise criteria for the occupied space.")
                if (!has(inputs, "r")) add("Aspect ratio not provided — assumed 1.0 (square equivalent).")
            },
            stepsAr = listOf(
                "المساحة: A = Q/v = ${Fmt.n(q, 5)} / ${Fmt.n(v, 3)} = ${Fmt.n(area, 5)} m²",
                "القطر الدائري المكافئ: D_eq = √(4A/π) = ${Fmt.n(dEq, 4)} m = ${Fmt.n(dEq * 1000.0, 0)} mm",
                "المستطيل (W/H = ${Fmt.n(ratio, 2)}): العرض = ${Fmt.n(w * 1000.0, 0)} mm، الارتفاع = ${Fmt.n(h * 1000.0, 0)} mm",
            ),
            warningsAr = buildList {
                add("الناتج هو الحد الأدنى النظري؛ اختر مقاس الدكت القياسي التالي المساوي أو الأكبر.")
                if (v > 8.0) {
                    add("سرعة مستهدفة أعلى من 8 m/s - راجع معايير الضوضاء للفراغ المأهول.")
                }
                if (!has(inputs, "r")) {
                    add("لم تُدخل نسبة الأبعاد - افتُرضت 1.0 (المكافئ المربع).")
                }
            },
        )
    }
}
