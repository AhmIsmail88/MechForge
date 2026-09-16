package com.mechforge.app.ui

import com.mechforge.app.ui.i18n.CalculatorArabic
import com.mechforge.core.engine.CalculatorDefinition

/**
 * Picks the Arabic content of a calculator when the interface is Arabic and falls back to the
 * engine's English text otherwise.
 *
 * Only text is translated: names, descriptions and the labels of inputs and results. The numbers,
 * the units, the formulas and every line of arithmetic come from the engine unchanged, so an
 * Arabic interface can never change a result.
 */
object CalcText {

    fun name(def: CalculatorDefinition, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.name(def.id, def.name) else def.name

    fun description(def: CalculatorDefinition, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.description(def.id, def.description) else def.description

    fun inputLabel(calculatorId: String, inputId: String, fallback: String, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.inputLabel(calculatorId, inputId, fallback) else fallback

    fun resultLabel(calculatorId: String, resultId: String, fallback: String, isArabic: Boolean): String =
        if (isArabic) CalculatorArabic.resultLabel(calculatorId, resultId, fallback) else fallback
}
