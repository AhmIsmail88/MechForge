package com.mechforge.app.ui

import com.mechforge.app.ui.i18n.CalculatorArabic
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputValue

/**
 * Picks the Arabic content of a calculator when the interface is Arabic and falls back to the
 * engine's English text otherwise.
 *
 * Only text is translated: names, descriptions and the labels of inputs and results. The numbers,
 * the units, the formulas and every line of arithmetic come from the engine unchanged, so an
 * Arabic interface can never change a result.
 */
object CalcText {

    fun categoryName(category: CalculatorCategory, isArabic: Boolean): String =
        if (!isArabic) category.displayName else when (category) {
            CalculatorCategory.HYDRAULICS -> "الهيدروليكا"
            CalculatorCategory.HVAC -> "التكييف والتهوية"
            CalculatorCategory.THERMODYNAMICS -> "الديناميكا الحرارية"
            CalculatorCategory.MECHANICAL_DESIGN -> "التصميم الميكانيكي"
            CalculatorCategory.PIPING -> "الأنابيب"
            CalculatorCategory.WATER_WASTEWATER -> "المياه والصرف الصحي"
            CalculatorCategory.FIRE_PROTECTION -> "الحماية من الحريق"
            CalculatorCategory.EQUIPMENT -> "المعدات"
            CalculatorCategory.UNIT_CONVERSION -> "تحويل الوحدات"
        }

    fun name(def: CalculatorDefinition, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.name(def.id, def.name) else def.name

    fun description(def: CalculatorDefinition, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.description(def.id, def.description) else def.description

    fun inputLabel(calculatorId: String, inputId: String, fallback: String, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.inputLabel(calculatorId, inputId, fallback) else fallback

    fun resultLabel(calculatorId: String, resultId: String, fallback: String, isArabic: Boolean): String =
        if (!isArabic) fallback else {
            val translated = CalculatorArabic.resultLabel(calculatorId, resultId, fallback)
            when {
                fallback.contains("Theoretical", ignoreCase = true) -> "تقدير نظري غير معتمد للتصميم — $translated"
                fallback.contains("Preliminary", ignoreCase = true) -> "نتيجة مبدئية تحتاج تحققًا — $translated"
                else -> translated
            }
        }

    /** Keep engine-generated assumption warnings when the calculator supplies Arabic warnings. */
    fun warnings(
        def: CalculatorDefinition,
        inputs: Map<String, InputValue>,
        output: CalcOutput,
        isArabic: Boolean,
    ): List<String> {
        if (!isArabic || output.warningsAr.isEmpty()) return output.warnings
        val assumptions = def.inputs
            .filter { it.id !in inputs }
            .mapNotNull { it.assumedWhenOmitted }
            .filter { it in output.warnings }
        val ownEnglish = output.warnings.filterNot { it in assumptions }
        // Unpaired legacy translations must never suppress an engineering warning.
        return if (ownEnglish.size == output.warningsAr.size) assumptions + output.warningsAr
        else (assumptions + output.warningsAr + ownEnglish).distinct()
    }
}
