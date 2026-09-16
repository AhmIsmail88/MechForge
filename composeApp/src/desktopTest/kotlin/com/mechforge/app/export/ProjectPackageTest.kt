package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering audit section 10: the package is what an office hands over, so its structure is
 * pinned here - cover, register, and one clean page per calculation, all from frozen records.
 */
class ProjectPackageTest {

    private val labels = ReportLabels.ENGLISH
    private val project = ProjectInfo(
        name = "Wastewater Pump Station 01",
        projectNumber = "WPS-001",
        client = "ABC",
        preparedBy = "A. Ismail",
    )

    private fun record(n: Int, status: String, title: String = "Pump power $n") = ProjectPackage.Record(
        calculationNumber = "C-%03d".format(n),
        calculatorId = "pump-power",
        title = title,
        revision = "0$n",
        status = status,
        timestamp = 1726200000000L + n * 86_400_000L,
        inputsJson = "{}",
        resultsJson = "{}",
    )

    @Test
    fun thePackageCoversTheProjectRegistersAndBreaksAPagePerCalculation() {
        val records = listOf(record(1, "Approved"), record(2, "For Review"))
        val blocks = ProjectPackage.build(project, records, labels, "2026-09-16")

        val headings = blocks.filterIsInstance<ReportBlock.Heading>().map { it.text }
        assertEquals(labels.packageTitle, headings.first(), "the package opens with its cover heading")
        assertTrue(headings.any { it == labels.registerTitle }, "the register has its own section")
        assertTrue(headings.any { it.startsWith("C-001") }, "each calculation gets a numbered section")
        assertTrue(headings.any { it.startsWith("C-002") })

        // one page break before the register and one before every calculation
        assertEquals(
            1 + records.size,
            blocks.count { it is ReportBlock.PageBreak },
            "the register and every calculation must start on a clean page",
        )
    }

    @Test
    fun theCoverCarriesTheProjectAndItsStatusSummary() {
        val records = listOf(record(1, "Approved"), record(2, "Approved"), record(3, "For Review"))
        val blocks = ProjectPackage.build(project, records, labels, "2026-09-16")
        val cover = blocks.takeWhile { it !is ReportBlock.PageBreak }
        val keyValues = cover.filterIsInstance<ReportBlock.KeyValue>().associate { it.label to it.value }

        assertEquals("Wastewater Pump Station 01", keyValues[labels.project])
        assertEquals("WPS-001", keyValues[labels.reportNo])
        assertEquals("ABC", keyValues[labels.client])
        assertEquals("3", keyValues[labels.registerTotal])
        assertEquals("2", keyValues["Approved"])
        assertEquals("1", keyValues["For Review"])
    }

    @Test
    fun aRecordThatCannotBeReplayedIsReportedInsteadOfPrintingABlankPage() {
        val blocks = ProjectPackage.build(project, listOf(record(4, "Draft")), labels, "2026-09-16")
        val warnings = blocks.filterIsInstance<ReportBlock.Warning>().map { it.text }
        assertTrue(
            labels.packageRecordUnavailable in warnings,
            "an unreplayable record must say so, got $warnings",
        )
    }

    @Test
    fun anEmptyProjectProducesACoverAndAnEmptyRegister() {
        val blocks = ProjectPackage.build(ProjectInfo(name = "Only a name"), emptyList(), labels, "2026-09-16")
        assertEquals(1, blocks.count { it is ReportBlock.PageBreak }, "only the register page break")
        assertEquals(
            "0",
            blocks.filterIsInstance<ReportBlock.KeyValue>().associate { it.label to it.value }[labels.registerTotal],
        )
        assertTrue(
            labels.registerEmpty in blocks.filterIsInstance<ReportBlock.Paragraph>().map { it.text },
            "an empty register says it is empty",
        )
    }

    @Test
    fun recordDatesAreReadInUtc() {
        assertEquals("2024-09-13", ProjectPackage.dateOf(1726200000000L))
        assertEquals("2024-09-14", ProjectPackage.dateOf(1726200000000L + 86_400_000L))
    }
}
