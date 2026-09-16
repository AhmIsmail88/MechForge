package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering audit section 12: the export checks warn about what is missing without ever
 * pretending a calculation is an approved design document.
 */
class ReportQaTest {

    private val full = ProjectInfo(
        name = "Wastewater Pump Station 01",
        projectNumber = "WPS-001",
        revision = "02",
        status = "For Review",
    )

    @Test
    fun aCompleteSavedSheetRaisesNothing() {
        val issues = ReportQa.check(full, "FP-004", "02", "For Review", emptyList(), isSaved = true)
        assertTrue(issues.isEmpty(), "a complete sheet must raise nothing, got $issues")
    }

    @Test
    fun aMissingProjectIsAWarning() {
        val noProject = ReportQa.check(null, "FP-004", "02", "Approved", emptyList(), isSaved = true)
        assertEquals(ReportQa.Severity.WARNING, noProject.first().severity)
        assertTrue(noProject.first().message.contains("No project"), "$noProject")

        val blankProject = ReportQa.check(ProjectInfo(), "FP-004", "02", "Approved", emptyList(), true)
        assertEquals(ReportQa.Severity.WARNING, blankProject.first().severity)
        assertTrue(blankProject.first().message.contains("still blank"), "$blankProject")
    }

    @Test
    fun missingDocumentFieldsAreReportedButNotAsBlockers() {
        val issues = ReportQa.check(full, null, "", "", emptyList(), isSaved = false)
        assertTrue(issues.all { it.severity == ReportQa.Severity.INFO }, "nothing here is a blocker: $issues")
        assertTrue(issues.any { it.message.contains("revision") }, "$issues")
        assertTrue(issues.any { it.message.contains("status") }, "$issues")
        assertTrue(issues.any { it.message.contains("register") }, "$issues")
    }

    @Test
    fun assumedValuesAreCountedAndFlagged() {
        val assumed = listOf(
            "Fluid density assumed as 1000 kg/m3 - use the value for the fluid actually handled.",
            "Overall efficiency assumed as 0.80 - use the manufacturer figure.",
        )
        val issues = ReportQa.check(full, "FP-004", "02", "For Review", assumed, isSaved = true)
        assertEquals(1, issues.size)
        assertTrue(issues.first().message.startsWith("2 assumed value(s)"), "$issues")
    }

    @Test
    fun theSummaryListsBlockersBeforeInformation() {
        val issues = ReportQa.check(null, null, "", "", listOf("Assumed X"), isSaved = false)
        val lines = ReportQa.summarise(issues)
        assertEquals(issues.size, lines.size)
        assertTrue(lines.first().startsWith("WARNING"), "warnings lead the list: $lines")
        assertEquals(
            lines.sortedBy { if (it.startsWith("WARNING")) 0 else 1 },
            lines,
            "the ordering must be stable and warning-first",
        )
        assertTrue(lines.all { it.contains(": ") }, "each line names its severity: $lines")
    }
}
