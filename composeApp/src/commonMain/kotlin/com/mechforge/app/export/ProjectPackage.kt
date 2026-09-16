package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue

/**
 * The project calculation package (engineering audit section 10).
 *
 * One document an office can hand over: a cover with the project data, the calculation
 * register, then every selected calculation on a clean page with its own number and revision.
 * Each calculation is rebuilt from the record saved at the time, so a later project revision
 * cannot change what the package says.
 *
 * The record is replayed through [decodeInputs] and [decodeOutput] so this builder stays free
 * of storage details and is testable on its own.
 */
object ProjectPackage {

    /** One saved calculation, as it was frozen in the database. */
    data class Record(
        val calculationNumber: String,
        val calculatorId: String,
        val title: String,
        val revision: String,
        val status: String,
        val timestamp: Long,
        val inputsJson: String,
        val resultsJson: String,
    )

    fun build(
        project: ProjectInfo,
        records: List<Record>,
        labels: ReportLabels,
        date: String,
        signature: List<Pair<String, String>> = emptyList(),
        decodeInputs: (String) -> Map<String, InputValue> = { emptyMap() },
        decodeOutput: (String) -> CalcOutput? = { null },
    ): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()

        // ---- cover -----------------------------------------------------------
        blocks += ReportBlock.Heading(labels.packageTitle)
        for ((label, value) in ReportMeta.build(project, labels, ReportMeta.Fallback(), date)) {
            blocks += ReportBlock.KeyValue(label, value)
        }
        blocks += ReportBlock.Divider
        blocks += ReportBlock.KeyValue(labels.registerTotal, records.size.toString())
        for ((status, count) in ProjectRegister.summarise(records.map { entryOf(it, project, date) })) {
            blocks += ReportBlock.KeyValue(status.ifBlank { labels.registerNoStatus }, count.toString())
        }
        if (signature.isNotEmpty()) {
            blocks += ReportBlock.Heading(labels.signatures)
            for ((role, name) in signature) {
                blocks += ReportBlock.TableRow(listOf(role, name.ifBlank { "............................" }))
            }
        }

        // ---- register --------------------------------------------------------
        blocks += ReportBlock.PageBreak
        blocks += ProjectRegister.build(
            project = project,
            entries = records.map { entryOf(it, project, date) },
            labels = labels,
            date = date,
        )

        // ---- one section per calculation, each on a clean page ---------------
        for (record in records) {
            blocks += ReportBlock.PageBreak
            blocks += ReportBlock.Heading(record.calculationNumber + "  |  " + record.title)
            blocks += ReportBlock.KeyValue(labels.revision, record.revision.ifBlank { "-" })
            blocks += ReportBlock.KeyValue(labels.status, record.status.ifBlank { "-" })
            blocks += ReportBlock.KeyValue(labels.date, dateOf(record.timestamp))
            blocks += ReportBlock.Divider

            val calculator = runCatching { CalculatorRegistry.byIdOrThrow(record.calculatorId) }.getOrNull()
            val output = decodeOutput(record.resultsJson)
            if (calculator != null && output != null) {
                blocks += ReportSheet.build(
                    calculator = calculator,
                    inputs = decodeInputs(record.inputsJson),
                    output = output,
                    title = record.title,
                    labels = labels,
                    signature = signature,
                    localizedName = { fallback ->
                        com.mechforge.app.ui.CalcText.name(calculator.def, labels == ReportLabels.ARABIC)
                    },
                    localizedLabel = { id, fallback ->
                        val inArabic = labels == ReportLabels.ARABIC
                        val isInput = calculator.def.inputs.any { it.id == id }
                        if (isInput) {
                            com.mechforge.app.ui.CalcText.inputLabel(calculator.def.id, id, fallback, inArabic)
                        } else {
                            com.mechforge.app.ui.CalcText.resultLabel(calculator.def.id, id, fallback, inArabic)
                        }
                    },
                )
            } else {
                // the record exists but cannot be replayed: say so instead of printing a blank page
                blocks += ReportBlock.Warning(labels.packageRecordUnavailable)
            }
        }

        return blocks
    }

    private fun entryOf(record: Record, project: ProjectInfo, date: String) = ProjectRegister.Entry(
        calculationNumber = record.calculationNumber,
        calculatorName = record.title,
        revision = record.revision,
        status = record.status,
        preparedBy = project.preparedBy,
        date = dateOf(record.timestamp),
    )

    /** ISO date from an epoch-millisecond timestamp, in UTC so a report never shifts by timezone. */
    fun dateOf(timestamp: Long): String =
        java.time.Instant.ofEpochMilli(timestamp)
            .atOffset(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .toString()
}
