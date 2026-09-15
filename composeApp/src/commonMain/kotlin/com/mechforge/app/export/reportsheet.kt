package com.mechforge.app.export

import com.mechforge.app.ui.util.UiFormat
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units

/**
 * Builds the structured content of one engineering calculation sheet from the
 * calculator definition, the entered inputs and the computed output.
 *
 * This replaces the earlier approach of parsing the plain-text report line by line:
 * the PDF now gets real tables (inputs, results), a formula block, the calculation
 * steps, warnings, notes and the reference - i.e. a complete engineering sheet
 * rather than a dumped text file.
 */
object ReportSheet {

    fun build(
        calculator: Calculator,
        inputs: Map<String, InputValue>,
        output: CalcOutput,
        title: String?,
    ): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()

        if (!title.isNullOrBlank() && title != calculator.def.name) {
            blocks += ReportBlock.Heading("Title")
            blocks += ReportBlock.TableRow(listOf("Title", title))
        }

        // Inputs -------------------------------------------------------------
        val inputRows = calculator.def.inputs.mapNotNull { spec ->
            val iv = inputs[spec.id] ?: return@mapNotNull null
            val unit = Units.byId(iv.displayUnitId)
            val value = "${UiFormat.n(unit.fromBase(iv.baseValue))} ${unit.symbol}"
            val label = if (spec.symbol.isBlank()) spec.label else "${spec.symbol}  ${spec.label}"
            ReportBlock.TableRow(listOf(label, value))
        }
        if (inputRows.isNotEmpty()) {
            blocks += ReportBlock.Heading("Inputs")
            blocks += inputRows.map { ReportBlock.TableRow(it.cells) }
        }

        // Formula ------------------------------------------------------------
        blocks += ReportBlock.Heading("Formula")
        blocks += ReportBlock.Paragraph(calculator.def.formulaDisplay)

        // Calculation steps --------------------------------------------------
        if (output.steps.isNotEmpty()) {
            blocks += ReportBlock.Heading("Calculation steps")
            for (step in output.steps) {
                blocks += ReportBlock.Paragraph(step)
            }
        }

        // Results ------------------------------------------------------------
        if (output.results.isNotEmpty()) {
            blocks += ReportBlock.Heading("Results")
            for (r in output.results) {
                val unit = Units.byId(r.unitId)
                val marker = when {
                    r.isRecommended -> "★ "
                    r.isPrimary -> ""
                    else -> ""
                }
                val label = marker + r.label
                val value = "${UiFormat.n(r.value)} ${unit.symbol}"
                blocks += ReportBlock.TableRow(listOf(label, value))
            }
        }

        // Warnings -----------------------------------------------------------
        if (output.warnings.isNotEmpty()) {
            blocks += ReportBlock.Heading("Warnings")
            for (w in output.warnings) {
                blocks += ReportBlock.Warning(w)
            }
        }

        // Notes + reference ---------------------------------------------------
        if (calculator.def.notes.isNotBlank()) {
            blocks += ReportBlock.Heading("Engineering notes")
            blocks += ReportBlock.Paragraph(calculator.def.notes)
        }
        blocks += ReportBlock.Heading("Reference")
        blocks += ReportBlock.Paragraph(calculator.def.reference)

        return blocks
    }
}
