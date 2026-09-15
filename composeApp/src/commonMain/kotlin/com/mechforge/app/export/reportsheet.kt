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
 * The PDF/report therefore gets real tables (inputs, results), a formula block, the
 * calculation steps, warnings, notes, the reference and a signature block - a complete
 * engineering sheet rather than a dumped text file.
 */
object ReportSheet {

    fun build(
        calculator: Calculator,
        inputs: Map<String, InputValue>,
        output: CalcOutput,
        title: String?,
        labels: ReportLabels = ReportLabels.ENGLISH,
        signature: List<Pair<String, String>> = emptyList(),
    ): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()

        if (!title.isNullOrBlank() && title != calculator.def.name) {
            blocks += ReportBlock.Heading(labels.documentTitle)
            blocks += ReportBlock.TableRow(listOf(labels.documentTitle, title))
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
            blocks += ReportBlock.Heading(labels.inputs)
            blocks += inputRows.map { ReportBlock.TableRow(it.cells) }
        }

        // Formula ------------------------------------------------------------
        blocks += ReportBlock.Heading(labels.formula)
        blocks += ReportBlock.Paragraph(calculator.def.formulaDisplay)

        // Calculation steps --------------------------------------------------
        if (output.steps.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.calculationSteps)
            for (step in output.steps) {
                blocks += ReportBlock.Paragraph(step)
            }
        }

        // Results ------------------------------------------------------------
        if (output.results.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.results)
            for (r in output.results) {
                val unit = Units.byId(r.unitId)
                val marker = if (r.isRecommended) "* " else ""
                val value = "${UiFormat.n(r.value)} ${unit.symbol}"
                blocks += ReportBlock.TableRow(listOf(marker + r.label, value))
            }
        }

        // Warnings -----------------------------------------------------------
        if (output.warnings.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.warnings)
            for (w in output.warnings) {
                blocks += ReportBlock.Warning(w)
            }
        }

        // Notes + reference ---------------------------------------------------
        if (calculator.def.notes.isNotBlank()) {
            blocks += ReportBlock.Heading(labels.notes)
            blocks += ReportBlock.Paragraph(calculator.def.notes)
        }
        blocks += ReportBlock.Heading(labels.reference)
        blocks += ReportBlock.Paragraph(calculator.def.reference)

        // Signatures ----------------------------------------------------------
        if (signature.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.signatures)
            for ((role, name) in signature) {
                blocks += ReportBlock.TableRow(listOf(role, name.ifBlank { "............................" }))
            }
            blocks += ReportBlock.Paragraph(
                "${labels.signatureLine}: ..............................        " +
                    "${labels.dateLine}: .................."
            )
        }

        return blocks
    }
}
