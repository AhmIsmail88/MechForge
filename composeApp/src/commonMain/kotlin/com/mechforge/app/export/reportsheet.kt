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
        documentControl: List<Pair<String, String>> = emptyList(),
        disclaimer: String? = null,
        localizedName: (String) -> String = { it },
        localizedLabel: (String, String) -> String = { _, fallback -> fallback },
    ): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()

        val sheetTitle = title ?: localizedName(calculator.def.name)
        if (!title.isNullOrBlank() && title != calculator.def.name) {
            blocks += ReportBlock.Heading(labels.documentTitle)
            blocks += ReportBlock.TableRow(listOf(labels.documentTitle, title))
        }

        // Inputs -------------------------------------------------------------
        val inputRows = calculator.def.inputs.mapNotNull { spec ->
            val iv = inputs[spec.id] ?: return@mapNotNull null
            val unit = Units.byId(iv.displayUnitId)
            val value = "${UiFormat.n(unit.fromBase(iv.baseValue))} ${unit.symbol}"
            val name = localizedLabel(spec.id, spec.label)
            val label = if (spec.symbol.isBlank()) name else "${spec.symbol}  $name"
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
        val arabicOutput = labels == ReportLabels.ARABIC
        val stepsToPrint = if (arabicOutput && output.stepsAr.isNotEmpty()) output.stepsAr else output.steps
        if (stepsToPrint.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.calculationSteps)
            for (step in stepsToPrint) {
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
                blocks += ReportBlock.TableRow(listOf(marker + localizedLabel(r.id, r.label), value))
            }
        }

        // Warnings -----------------------------------------------------------
        val warningsToPrint = if (arabicOutput && output.warningsAr.isNotEmpty()) output.warningsAr else output.warnings
        if (warningsToPrint.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.warnings)
            for (w in warningsToPrint) {
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

        // Document control ----------------------------------------------------
        val control = documentControl.filter { it.second.isNotBlank() }
        if (control.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.documentControl)
            for ((label, value) in control) {
                blocks += ReportBlock.TableRow(listOf(label, value))
            }
        }

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
            // Credit line printed under the signature block.
            blocks += ReportBlock.Paragraph("By Ahmed Ismail")
        }

        // Result versus approval (engineering audit section 9): the sheet states what it is.
        if (!disclaimer.isNullOrBlank()) {
            blocks += ReportBlock.Paragraph(disclaimer)
        }

        return blocks
    }
}
