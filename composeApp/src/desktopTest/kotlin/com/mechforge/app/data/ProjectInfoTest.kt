package com.mechforge.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The project record is engineering data that a report prints and a saved calculation freezes
 * (engineering audit sections 4, 5, 11), so its serialization contract is worth pinning.
 */
class ProjectInfoTest {

    private val full = ProjectInfo(
        name = "Wastewater Pump Station 01",
        projectNumber = "WPS-001",
        location = "Riyadh",
        client = "ABC",
        consultant = "XYZ",
        preparedBy = "A. Ismail",
        revision = "02",
        status = "For Review",
        codes = "NFPA 20, NFPA 12",
        codeEdition = "2022",
        notes = "Pilot project",
    )

    @Test
    fun roundTripsThroughJsonWithoutLosingFields() {
        val restored = ProjectInfo.fromJson(full.toJson())
        assertEquals(full, restored, "the snapshot must survive a save and a reload unchanged")
    }

    @Test
    fun anAbsentSnapshotIsNotAnError() {
        assertNull(ProjectInfo.fromJson(null))
        assertNull(ProjectInfo.fromJson(""))
        assertNull(ProjectInfo.fromJson("{ not json"))
    }

    @Test
    fun unknownFieldsAreIgnoredSoAnOlderBuildCanReadANewerSnapshot() {
        val restored = ProjectInfo.fromJson("""{"name":"P1","someFutureField":42}""")
        assertEquals("P1", restored?.name)
        assertEquals("", restored?.client)
    }

    @Test
    fun reportRowsPrintOnlyWhatWasFilledIn() {
        val rows = full.reportRows().toMap()
        assertEquals("WPS-001", rows["Project No."])
        assertEquals("ABC", rows["Client"])
        assertEquals("NFPA 20, NFPA 12", rows["Applicable Codes"])
        assertTrue(rows.keys.none { it == "Approved By" }, "an empty field must not reach the report: $rows")
        assertTrue(rows.keys.none { it == "Contractor" })

        val empty = ProjectInfo(name = "Only a name")
        assertEquals(listOf("Project" to "Only a name"), empty.reportRows())
    }

    @Test
    fun anEmptyProjectIsRecognisedSoTheReportCanFallBack() {
        assertTrue(ProjectInfo().isEmpty)
        assertTrue(ProjectInfo(name = "Named but otherwise blank").isEmpty)
        assertTrue(!full.isEmpty)
    }
}