package com.mechforge.app.export

import com.mechforge.app.ui.CalcText
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * An Arabic report must read as Arabic - the calculator name and the input and result labels - while
 * the numbers, the units and the formulas stay in their international form.
 */
class ReportSheetArabicTest {

    private val labels = ReportLabels.ARABIC
    private val calculator = CalculatorRegistry.byIdOrThrow("pump-power")

    private val inputs = mapOf(
        "q" to InputValue("q", 0.05, "m3h"),
        "h" to InputValue("h", 40.0, "m"),
        "eta" to InputValue("eta", 0.78, "pct"),
        "rho" to InputValue("rho", 1000.0, "kgm3"),
    )

    private fun sheetText(localize: Boolean): String {
        val out = calculator.run(inputs)
        val blocks = ReportSheet.build(
            calculator = calculator,
            inputs = inputs,
            output = out,
            title = null,
            labels = labels,
            localizedName = { fallback -> if (localize) CalcText.name(calculator.def, true) else fallback },
            localizedLabel = { id, fallback ->
                if (!localize) {
                    fallback
                } else if (calculator.def.inputs.any { it.id == id }) {
                    CalcText.inputLabel(calculator.def.id, id, fallback, true)
                } else {
                    CalcText.resultLabel(calculator.def.id, id, fallback, true)
                }
            },
        )
        return blocks.flatMap { block ->
            when (block) {
                is ReportBlock.Heading -> listOf(block.text)
                is ReportBlock.Paragraph -> listOf(block.text)
                is ReportBlock.TableRow -> block.cells
                is ReportBlock.KeyValue -> listOf(block.label, block.value)
                else -> emptyList()
            }
        }.joinToString(" | ")
    }

    @Test
    fun anArabicSheetCarriesTheArabicLabels() {
        val text = sheetText(localize = true)
        assertTrue(text.contains("معدل السريان"), "the Arabic input label is missing: $text")
        assertTrue(text.contains("الرفع الكلي"), "the Arabic input label is missing: $text")
        assertTrue(text.contains("القدرة الهيدروليكية"), "the Arabic result label is missing: $text")
        assertTrue(text.contains("محرك") || text.contains("المحرك"), "the Arabic result label is missing: $text")
    }

    @Test
    fun theNumbersUnitsAndFormulaStayInternational() {
        val text = sheetText(localize = true)
        assertTrue(text.contains("m3/h") || text.contains("m³"), "the unit symbols must stay: $text")
        assertTrue(text.contains("kW"), "the result unit must stay: $text")
        assertTrue(Regex("[0-9]\\.[0-9]").containsMatchIn(text), "the values must print as numbers: $text")
        assertTrue(text.contains(calculator.def.formulaDisplay), "the formula is never translated: $text")
        assertTrue(
            text.contains("Q") || text.contains("P"),
            "the symbols in the formula and the steps stay as the engine wrote them: $text",
        )
    }

    @Test
    fun withoutTheLocalizerTheSheetIsUnchanged() {
        val text = sheetText(localize = false)
        assertTrue(text.contains("Flow rate"), "the English label must remain when nothing is localized: $text")
        assertTrue(!text.contains("معدل السريان"), "nothing Arabic should appear without the localizer")
    }
}
