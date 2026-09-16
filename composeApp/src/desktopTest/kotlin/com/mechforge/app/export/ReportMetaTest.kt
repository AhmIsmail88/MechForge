package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering audit section 11: the project data must reach the export automatically, and an
 * installation that never used the project area must keep printing its Settings letterhead.
 */
class ReportMetaTest {

    private val labels = ReportLabels.ENGLISH
    private val fallback = ReportMeta.Fallback(
        project = "Old project name",
        client = "Old client",
        engineer = "A. Ismail",
        location = "Cairo",
        reportNo = "R-001",
        revision = "01",
        code = "NFPA 20",
    )
    private val date = "2026-09-16"

    private fun rows(project: ProjectInfo?) =
        ReportMeta.build(project, labels, fallback, date).toMap()

    @Test
    fun aFilledProjectDrivesTheReportInsteadOfSettings() {
        val project = ProjectInfo(
            name = "Wastewater Pump Station 01",
            projectNumber = "WPS-001",
            client = "ABC",
            consultant = "XYZ",
            contractor = "DEF",
            location = "Riyadh",
            revision = "02",
            status = "For Review",
            preparedBy = "A. Ismail",
            documentNumber = "WPS-DOC-001",
            codes = "NFPA 20, NFPA 12",
            codeEdition = "2022",
        )
        val meta = rows(project)
        assertEquals("Wastewater Pump Station 01", meta[labels.project])
        assertEquals("WPS-001", meta[labels.reportNo])
        assertEquals("ABC", meta[labels.client])
        assertEquals("XYZ", meta[labels.consultant])
        assertEquals("DEF", meta[labels.contractor])
        assertEquals("Riyadh", meta[labels.location])
        assertEquals("02", meta[labels.revision])
        assertEquals("For Review", meta[labels.status])
        assertEquals("WPS-DOC-001", meta[labels.documentNo])
        assertEquals("NFPA 20, NFPA 12 - 2022", meta[labels.code], "the edition belongs with the codes")
        assertEquals(date, meta[labels.date])
        assertTrue(
            meta.values.none { it == "Old project name" || it == "Old client" },
            "Settings values must not leak once the project is filled in: $meta",
        )
    }

    @Test
    fun aBlankProjectFallsBackToTheSettingsValues() {
        val meta = rows(ProjectInfo(name = "Named but blank"))
        assertEquals("Named but blank", meta[labels.project], "the project name still wins")
        assertEquals("Old client", meta[labels.client])
        assertEquals("Cairo", meta[labels.location])
        assertEquals("NFPA 20", meta[labels.code])

        val noProject = rows(null)
        assertEquals("Old project name", noProject[labels.project])
        assertEquals("R-001", noProject[labels.reportNo])
    }

    @Test
    fun emptyFieldsNeverReachTheReport() {
        val meta = rows(ProjectInfo(name = "P1", client = "ABC"))
        assertTrue(meta.keys.none { it == labels.consultant }, "an empty consultant must be skipped: $meta")
        assertTrue(meta.keys.none { it == labels.status })
        assertTrue(meta.keys.none { it == labels.documentNo })
        assertEquals(date, meta[labels.date], "the date is always printed")
    }

    @Test
    fun codeAndEditionAreJoinedOnlyWhenBothExist() {
        // a project that carries other data is a filled record, so its own code line is used
        assertEquals(
            "NFPA 12",
            rows(ProjectInfo(name = "P", client = "ABC", codes = "NFPA 12"))[labels.code],
        )
        assertEquals(
            "2018",
            rows(ProjectInfo(name = "P", client = "ABC", codeEdition = "2018"))[labels.code],
        )
        assertTrue(
            rows(ProjectInfo(name = "P", client = "ABC")).keys.none { it == labels.code },
            "no code line when the project states neither",
        )
        // still a blank record: the Settings code stays, because nothing better is known yet
        assertEquals("NFPA 20", rows(ProjectInfo(name = "P")).getValue(labels.code))
    }
}