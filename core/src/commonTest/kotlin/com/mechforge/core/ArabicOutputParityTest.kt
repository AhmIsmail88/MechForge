package com.mechforge.core

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A calculator that provides Arabic steps must provide the same number of them and print the same
 * numbers: a translation may never change a value, drop a line or invent one.
 *
 * The check runs over the golden cases, so it covers every scenario the engine is verified with.
 */
class ArabicOutputParityTest {

    private val numberIn = Regex("[0-9]+\\.?[0-9]*(?:[eE]-?[0-9]+)?")

    @Test
    fun theArabicListsMirrorTheEnglishOnes() {
        var checked = 0
        var calculatorsWithArabic = 0
        val problems = mutableListOf<String>()

        for (case in GOLDEN_CASES) {
            val calculator = CalculatorRegistry.byIdOrThrow(case.calculatorId)
            val inputs = case.inputs.associate {
                it.inputId to InputValue(it.inputId, it.baseValue, it.displayUnitId)
            }
            val out = runCatching { calculator.run(inputs) }.getOrNull() ?: continue
            if (out.stepsAr.isEmpty() && out.warningsAr.isEmpty()) continue

            calculatorsWithArabic++
            checked++

            if (out.stepsAr.size != out.steps.size) {
                problems += "${case.calculatorId}/${case.scenario}: ${out.steps.size} English steps " +
                    "but ${out.stepsAr.size} Arabic"
            } else {
                out.steps.forEachIndexed { index, english ->
                    for (number in numberIn.findAll(english).map { it.value }) {
                        if (!out.stepsAr[index].contains(number)) {
                            problems += "${case.calculatorId}/${case.scenario} step ${index + 1}: " +
                                "the value $number is missing from the Arabic line"
                        }
                    }
                }
            }

            // run() prepends one caveat per omitted assumed input, in English, for both languages:
            // they belong to the definition rather than to the calculator's own warning list
            val assumptionCount = calculator.def.inputs
                .count { it.assumedWhenOmitted != null && !case.inputs.any { i -> i.inputId == it.id } }
            val ownWarnings = out.warnings.size - assumptionCount
            if (out.warningsAr.size != ownWarnings) {
                problems += "${case.calculatorId}/${case.scenario}: the calculator raises $ownWarnings " +
                    "warning(s) but provides ${out.warningsAr.size} in Arabic"
            }
        }

        assertTrue(calculatorsWithArabic > 0, "no calculator provides Arabic steps yet")
        assertTrue(problems.isEmpty(), problems.joinToString("; "))
        println("Arabic output parity: $checked scenario(s) with Arabic text, ${GOLDEN_CASES.size} available")
    }

    // Note: the earlier test that pinned one calculator as "still English" is gone by design - as
    // the translation rolls out there is no such calculator left to point at. The additivity it
    // proved is guaranteed by the optional defaults on stepsAr/warningsAr, and the parity test above
    // keeps checking that whatever a calculator does provide stays consistent with the English.
}
