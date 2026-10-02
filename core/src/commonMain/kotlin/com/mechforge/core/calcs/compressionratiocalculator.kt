package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/** Compression ratio and the number of identical stages. */
private val Def = CalculatorDefinition(
    id = "compression-ratio",
    name = "Compression Ratio (Multi-Stage)",
    category = CalculatorCategory.EQUIPMENT,
    description = "Overall compression ratio and the number of identical stages required when the per-stage ratio is limited.",
    formulaDisplay = "CR = P2/P1 ;  n = ceil(ln(CR)/ln(CR_max)) ;  CR_stage = CR^(1/n)",
    reference = "Compressor staging practice (e.g. GPSA Engineering Data Book); equal per-stage ratios with intercooling back to the inlet temperature.",
    notes = "Assumes each stage has the same ratio and that intercooling returns the gas to the inlet temperature (the usual basis for minimum work). Discharge temperatures must still be checked against the material limits.",
    keywords = listOf("compressor", "compression ratio", "staging", "intercooler", "equipment", "gas"),
    inputs = listOf(
        InputSpec("p1", "Absolute inlet pressure", "P1", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("p2", "Absolute discharge pressure", "P2", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("crmax", "Maximum per-stage ratio", "CR_max", UnitFamily.DIMENSIONLESS, required = false, minValue = 1.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("t1", "Inlet temperature (for the temperature limit)", "T1", UnitFamily.TEMPERATURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
        InputSpec("t2max", "Maximum allowable discharge temperature", "T2max", UnitFamily.TEMPERATURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c",
            assumedWhenOmitted = "No discharge-temperature limit given: the per-stage ratio falls back to the entered CR_max. The limit is what actually caps a stage, since CR_max = (T2max/T1)^(n/(n-1))."),
        InputSpec("n", "Compression exponent n (for the limit)", "n", UnitFamily.DIMENSIONLESS, required = false, minValue = 1.0, exclusiveMin = true, defaultUnitId = "dash",
            assumedWhenOmitted = "Exponent n assumed as 1.3, typical of a polytropic air compression; n = 1.4 is the isentropic value. It changes the ratio the temperature limit allows."),
    ),
)

object CompressionRatioCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p1 = value(inputs, "p1")
        val p2 = value(inputs, "p2")
        // The per-stage limit is the discharge temperature, not an arbitrary ratio:
        // CR_max = (T2max/T1)^(n/(n-1)). When those three are given the ratio is derived from them;
        // otherwise the entered CR_max stands, and 4.0 if nothing is entered at all.
        val nExp = if (has(inputs, "n")) value(inputs, "n") else 1.3
        val crFromLimit = if (has(inputs, "t1") && has(inputs, "t2max")) {
            (value(inputs, "t2max") / value(inputs, "t1")).pow(nExp / (nExp - 1.0))
        } else null
        if (has(inputs, "t1") != has(inputs, "t2max")) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("t2max", "Provide both inlet temperature and maximum discharge temperature.")))
        }
        val enteredLimit = inputs["crmax"]?.baseValue
        val crMax = if (crFromLimit != null) minOf(crFromLimit, enteredLimit ?: Double.POSITIVE_INFINITY) else (enteredLimit ?: 4.0)
        if (!crMax.isFinite() || crMax <= 1.0) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("crmax", "Per-stage ratio must exceed 1 and maximum discharge temperature must exceed inlet temperature.")))
        }

        if (p2 < p1) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("p2", "Discharge pressure must be greater than or equal to the inlet pressure."))
            )
        }

        val ratio = p2 / p1
        val stageCount = if (ratio <= crMax) 1.0 else ceil(ln(ratio) / ln(crMax))
        if (!stageCount.isFinite() || stageCount > Int.MAX_VALUE) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("crmax", "The specified stage limit produces an unsupported number of stages.")))
        }
        val stages = stageCount.toInt()
        val perStage = ratio.pow(1.0 / stages)
        val interstage = p1 * perStage

        return CalcOutput(
            results = listOf(
                result("cr", "Overall Compression Ratio", ratio, "dash", isPrimary = true),
                result("n", "Stages Required", stages.toDouble(), "dash", isPrimary = true),
                result("crStage", "Per-Stage Ratio", perStage, "dash", isPrimary = true),
                result("pint", "First Interstage Pressure", interstage / 1e5, "bar"),
            ),
            steps = listOf(
                "Overall ratio: CR = P2/P1 = ${Fmt.n(p2 / 1e5, 3)} / ${Fmt.n(p1 / 1e5, 3)} = ${Fmt.n(ratio, 3)}",
                "Maximum per-stage ratio: ${Fmt.n(crMax, 2)}",
                "Per-stage ratio from the temperature limit: CR_max = (T2max/T1)^(n/(n-1)) = ${Fmt.n(crFromLimit ?: crMax, 2)}${if (crFromLimit != null) " (derived from the discharge limit)" else " (ratio entered or assumed)"}",
                if (stages == 1) {
                    "CR <= CR_max - a single stage is sufficient."
                } else {
                    "Stages: n = ceil(ln(${Fmt.n(ratio, 3)})/ln(${Fmt.n(crMax, 2)})) = ${Fmt.n(ln(ratio) / ln(crMax), 3)} -> $stages stages"
                },
                "Equal per-stage ratio: CR_stage = CR^(1/n) = ${Fmt.n(ratio, 3)}^(1/$stages) = ${Fmt.n(perStage, 3)}",
                "First interstage pressure (after intercooling): ${Fmt.n(interstage / 1e5, 3)} bar",
            ),
            warnings = buildList {
                if (!has(inputs, "crmax") && crFromLimit == null) add("Maximum per-stage ratio assumed as 4.0 (typical air compressors; verify with the machine data).")
                if (stages > 4) add("More than 4 stages: consider a different machine type or review the required discharge pressure.")
                add("Check the discharge temperature and the intercooler duty for each stage before selecting the machine.")
            },
            stepsAr = listOf(
                "النسبة الكلية: CR = P2/P1 = ${Fmt.n(p2 / 1e5, 3)} / ${Fmt.n(p1 / 1e5, 3)} = ${Fmt.n(ratio, 3)}",
                "أقصى نسبة لكل مرحلة: ${Fmt.n(crMax, 2)}",
                "أقصى نسبة من حد الحرارة: CR_max = (T2max/T1)^(n/(n-1)) = ${Fmt.n(crFromLimit ?: crMax, 2)}${if (crFromLimit != null) " (مشتقة من حد التصريف)" else " (نسبة مُدخلة أو مفترضة)"}",
                if (stages == 1) {
                    "CR ≤ CR_max - مرحلة واحدة كافية."
                } else {
                    "المراحل: n = ceil(ln(${Fmt.n(ratio, 3)})/ln(${Fmt.n(crMax, 2)})) = ${Fmt.n(ln(ratio) / ln(crMax), 3)} ← $stages مرحلة"
                },
                "النسبة المتساوية لكل مرحلة: CR_stage = CR^(1/n) = ${Fmt.n(ratio, 3)}^(1/$stages) = ${Fmt.n(perStage, 3)}",
                "ضغط ما بين المرحلتين (بعد التبريد البيني): ${Fmt.n(interstage / 1e5, 3)} bar",
            ),
            warningsAr = buildList {
                if (!has(inputs, "crmax") && crFromLimit == null) {
                    add("لم تُدخل أقصى نسبة لكل مرحلة - افتُرضت 4.0 (ضواغط هواء معتادة؛ تحقق من بيانات الماكينة).")
                }
                if (stages > 4) {
                    add("أكثر من 4 مراحل: فكّر في نوع ماكينة مختلف أو راجع ضغط التصريف المطلوب.")
                }
                add("راجع حرارة التصريف وحمل المبرد البيني لكل مرحلة قبل اختيار الماكينة.")
            },
        )
    }
}
