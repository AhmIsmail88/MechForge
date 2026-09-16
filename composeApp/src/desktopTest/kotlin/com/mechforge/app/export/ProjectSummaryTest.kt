package com.mechforge.app.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering audit section 7.1: the project dashboard numbers, tested without a database.
 */
class ProjectSummaryTest {

    private val disciplines = mapOf(
        "pump-power" to "Hydraulics",
        "duct-sizing" to "HVAC",
        "fan-power" to "HVAC",
        "duct-pressure-loss" to "HVAC",
        "fm200-agent-quantity" to "Fire Protection",
    )

    private fun disciplineOf(id: String): String? = disciplines[id]

    @Test
    fun disciplinesAreCountedAndTheBusiestComesFirst() {
        val out = ProjectSummary.disciplines(
            calculatorIds = listOf(
                "duct-sizing", "fan-power", "pump-power", "duct-sizing", "fm200-agent-quantity",
                "duct-pressure-loss",
            ),
            disciplineOf = ::disciplineOf,
            unknownLabel = "Unknown",
        )
        assertEquals(
            listOf(
                ProjectSummary.Discipline("HVAC", 4),
                ProjectSummary.Discipline("Fire Protection", 1),
                ProjectSummary.Discipline("Hydraulics", 1),
            ),
            out,
            "the busiest discipline leads, then the rest alphabetically",
        )
    }

    @Test
    fun anUnknownCalculatorIsCollectedNotDropped() {
        val out = ProjectSummary.disciplines(
            calculatorIds = listOf("gone-from-the-registry", "duct-sizing"),
            disciplineOf = ::disciplineOf,
            unknownLabel = "No discipline",
        )
        // equal counts are ordered alphabetically, so compare the content here and leave the
        // ordering assertion to the test above
        assertEquals(
            setOf(
                ProjectSummary.Discipline("HVAC", 1),
                ProjectSummary.Discipline("No discipline", 1),
            ),
            out.toSet(),
            "a record whose calculator is unknown must still be counted: $out",
        )

        // a blank discipline name is treated as unknown too
        val blank = ProjectSummary.disciplines(listOf("x"), { "" }, "Unknown")
        assertEquals(listOf(ProjectSummary.Discipline("Unknown", 1)), blank)
        assertTrue(ProjectSummary.disciplines(emptyList(), ::disciplineOf, "Unknown").isEmpty())
    }

    @Test
    fun theRecentListKeepsTheGivenOrderAndRespectsTheLimit() {
        val records = (1..8).map { Triple("C-%03d".format(it), "Calculation $it", "2026-09-0$it") }
        val recent = ProjectSummary.recent(records, limit = 5)
        assertEquals(5, recent.size)
        assertEquals("C-001", recent.first().label, "the caller's newest-first order is preserved")
        assertEquals("Calculation 1", recent.first().title)
        assertEquals("2026-09-01", recent.first().date)
        assertEquals("C-005", recent.last().label)
        assertTrue(ProjectSummary.recent(records, limit = 0).isEmpty())
        assertEquals(8, ProjectSummary.recent(records, limit = 50).size)
    }

    @Test
    fun statusPercentagesAreWholeNumbersThatAddUp() {
        val counts = listOf("Approved" to 2, "For Review" to 1, "Draft" to 1)
        val percents = ProjectSummary.statusPercent(counts)
        assertEquals(listOf("Approved" to 50, "For Review" to 25, "Draft" to 25), percents)
        assertEquals(100, percents.sumOf { it.second })

        assertTrue(ProjectSummary.statusPercent(emptyList()).isEmpty())
        assertTrue(ProjectSummary.statusPercent(listOf("Approved" to 0)).isEmpty())
    }
}
