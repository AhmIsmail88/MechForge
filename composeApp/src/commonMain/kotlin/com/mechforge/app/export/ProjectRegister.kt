package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo

/**
 * The project calculation register (engineering audit section 9).
 *
 * One row per saved calculation: its number, the calculator, the revision, the status, who
 * prepared it and when. This is the sheet an engineering office hands over with a calculation
 * package, so it is built from the same ReportBlock model as a calculation sheet and therefore
 * exports to PDF and Excel unchanged.
 */
object ProjectRegister {

    /** One saved calculation, already reduced to what the register prints. */
    data class Entry(
        val calculationNumber: String,
        val calculatorName: String,
        val revision: String,
        val status: String,
        val preparedBy: String,
        val date: String,
    )

    fun build(
        project: ProjectInfo,
        entries: List<Entry>,
        labels: ReportLabels,
        date: String,
    ): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Heading(labels.registerTitle)

        // the same labelled block a calculation sheet prints, so the register is localized with
        // the report language instead of carrying hard-coded English headings
        for ((label, value) in ReportMeta.build(project, labels, ReportMeta.Fallback(), date)) {
            blocks += ReportBlock.KeyValue(label, value)
        }
        blocks += ReportBlock.Divider

        blocks += ReportBlock.Heading(labels.registerSummary)
        blocks += ReportBlock.KeyValue(labels.registerTotal, entries.size.toString())
        for ((status, count) in summarise(entries)) {
            blocks += ReportBlock.KeyValue(status.ifBlank { labels.registerNoStatus }, count.toString())
        }
        blocks += ReportBlock.Divider

        blocks += ReportBlock.Heading(labels.registerEntries)
        blocks += ReportBlock.TableRow(
            listOf(
                labels.registerCalcNo,
                labels.registerCalculator,
                labels.revision,
                labels.status,
                labels.registerPreparedBy,
                labels.date,
            )
        )
        for (entry in entries) {
            blocks += ReportBlock.TableRow(
                listOf(
                    entry.calculationNumber,
                    entry.calculatorName,
                    entry.revision,
                    entry.status.ifBlank { "-" },
                    entry.preparedBy,
                    entry.date,
                )
            )
        }
        if (entries.isEmpty()) {
            blocks += ReportBlock.Paragraph(labels.registerEmpty)
        }

        return blocks
    }

    /** Counts per status, largest first, with an empty status collected under one heading. */
    fun summarise(entries: List<Entry>): List<Pair<String, Int>> =
        entries.groupingBy { it.status }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
}