package com.mechforge.app.export

import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import com.mechforge.app.ui.util.UiFormat

/**
 * Plain-text calculation report (README v2 §36 text export for the pilot).
 *
 * Platform-agnostic on purpose: this object only builds the report text. Saving it
 * is the job of the platform [ReportExporter] (desktop save dialog, Android app
 * files directory).
 */
object ReportWriter {

    fun build(
        calculator: Calculator,
        inputs: Map<String, InputValue>,
        output: CalcOutput,
        title: String?,
    ): String = buildString {
        appendLine("MechForge — Calculation Report")
        appendLine("================================")
        appendLine("Calculator : ${calculator.def.name}")
        appendLine("Category   : ${calculator.def.category.displayName}")
        appendLine("Title      : ${title ?: calculator.def.name}")
        appendLine("Date       : ${java.time.LocalDateTime.now()}")
        appendLine()
        appendLine("INPUTS")
        appendLine("------")
        for (spec in calculator.def.inputs) {
            val iv = inputs[spec.id] ?: continue
            val unit = Units.byId(iv.displayUnitId)
            appendLine("${spec.symbol} ${spec.label} = ${UiFormat.n(unit.fromBase(iv.baseValue))} ${unit.symbol}")
        }
        appendLine()
        appendLine("FORMULA")
        appendLine("-------")
        appendLine(calculator.def.formulaDisplay)
        appendLine()
        appendLine("CALCULATION")
        appendLine("-----------")
        for (step in output.steps) appendLine(step)
        appendLine()
        appendLine("RESULTS")
        appendLine("-------")
        for (r in output.results) {
            val marker = when {
                r.isRecommended -> "  [recommended]"
                r.isPrimary -> "  [primary]"
                else -> ""
            }
            appendLine("${r.label} = ${UiFormat.n(r.value)} ${Units.byId(r.unitId).symbol}$marker")
        }
        if (output.warnings.isNotEmpty()) {
            appendLine()
            appendLine("WARNINGS")
            appendLine("--------")
            for (w in output.warnings) appendLine("! $w")
        }
        appendLine()
        appendLine("REFERENCE")
        appendLine("---------")
        appendLine(calculator.def.reference)
        if (calculator.def.notes.isNotBlank()) {
            appendLine()
            appendLine("NOTES")
            appendLine("-----")
            appendLine(calculator.def.notes)
        }
    }
}
