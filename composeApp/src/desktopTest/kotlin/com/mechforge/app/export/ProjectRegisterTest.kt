package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering audit section 9: the project calculation register is an engineering deliverable,
 * so its content and its status summary are pinned here.
 */
class ProjectRegisterTest {

    private val labels = ReportLabels.ENGLISH
    private val project = ProjectInfo(
        name = "Wastewater Pump Station 01",
        projectNumber = "WPS-001",
        client = "ABC",
        codeEdition = "2022",
        codes = "NFPA 20",
    )

    private val entries = listOf(
        ProjectRegister.Entry("HVAC-001", "Duct Sizing", "01", "Approved", "A. Ismail", "2026-09-01"),
        ProjectRegister.Entry("HVAC-002", "Fan Power", "01", "For Review", "A. Ismail", "2026-09-02"),
        ProjectRegister.Entry("FP-001", "Fire Pump Head", "02", "Approved", "A. Ismail", "2026-09-03"),
        ProjectRegister.Entry("HYD-004", "Pipe Velocity", "01", "", "A. Ismail", "2026-09-04"),
    )

    @Test
    fun everyEntryReachesTheRegisterInOrder() {
        val blocks = ProjectRegister.build(project, entries, labels, "2026-09-16")
        val rows = blocks.filterIsInstance<ReportBlock.TableRow>().map { it.cells }
        assertEquals(
            listOf(
                labels.registerCalcNo, labels.registerCalculator, labels.revision,
                labels.status, labels.registerPreparedBy, labels.date,
            ),
            rows.first(),
            "the header row must name every column",
        )
        val body = rows.drop(1)
        assertEquals(4, body.size)
        assertEquals(listOf("HVAC-001", "Duct Sizing", "01", "Approved", "A. Ismail", "2026-09-01"), body[0])
        assertEquals(listOf("FP-001", "Fire Pump Head", "02", "Approved", "A. Ismail", "2026-09-03"), body[2])
        assertEquals("-", body[3][3], "a record without a status prints a dash, not an empty cell")
    }

    @Test
    fun theRegisterCarriesTheProjectAndItsStatusSummary() {
        val blocks = ProjectRegister.build(project, entries, labels, "2026-09-16")
        val keyValues = blocks.filterIsInstance<ReportBlock.KeyValue>().associate { it.label to it.value }
        assertEquals("Wastewater Pump Station 01", keyValues[labels.project])
        assertEquals("WPS-001", keyValues[labels.reportNo])
        assertEquals("ABC", keyValues[labels.client])
        assertEquals("4", keyValues[labels.registerTotal])
        assertEquals("2", keyValues["Approved"], "two calculations stand approved: $keyValues")
        assertEquals("1", keyValues["For Review"])
        assertEquals("1", keyValues[labels.registerNoStatus], "an empty status is summarised, not dropped")
        assertEquals("2026-09-16", keyValues[labels.date])
    }

    @Test
    fun anEmptyProjectSaysSoInsteadOfPrintingABareTable() {
        val blocks = ProjectRegister.build(ProjectInfo(name = "P1"), emptyList(), labels, "2026-09-16")
        val paragraphs = blocks.filterIsInstance<ReportBlock.Paragraph>().map { it.text }
        assertTrue(labels.registerEmpty in paragraphs, "an empty register must say it is empty")
        assertEquals(
            "0",
            blocks.filterIsInstance<ReportBlock.KeyValue>().associate { it.label to it.value }[labels.registerTotal],
        )
        assertEquals(0, ProjectRegister.summarise(emptyList()).size)
    }
}