package com.mechforge.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mechforge.app.data.HistoryRepository
import com.mechforge.app.data.ProjectInfo
import com.mechforge.app.data.ProjectsRepository
import com.mechforge.app.data.Snapshots
import com.mechforge.app.export.ProjectPackage
import com.mechforge.app.export.ProjectRegister
import com.mechforge.app.export.ReportLabels
import com.mechforge.app.export.ReportMeta
import com.mechforge.app.export.ReportQa
import com.mechforge.app.export.ReportBlock
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.db.MechForgeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The whole project workflow on one database: create a project, fill its engineering record,
 * make it active, save a calculation against it, then read it back and build the three report
 * artefacts (sheet meta, register, package) from what was actually stored.
 *
 * This is the integration proof for the engineering-audit work: the earlier tests cover each
 * piece, this one shows the pieces hand data to each other.
 */
class ProjectWorkflowTest {

    private fun database(): MechForgeDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MechForgeDatabase.Schema.create(driver)
        return MechForgeDatabase(driver)
    }

    @Test
    fun aProjectCarriesItsCalculationsIntoTheRegisterAndThePackage() {
        val db = database()
        val projects = ProjectsRepository(db)
        val history = HistoryRepository(db)

        // ---- the engineer creates the project and fills the engineering record ----------
        projects.create("Wastewater Pump Station 01", "", 1_726_200_000_000L)
        val projectId = db.projectsQueries.selectAllProjects().executeAsList().first().id
        val record = ProjectInfo(
            name = "Wastewater Pump Station 01",
            description = "Fire protection package",
            projectNumber = "WPS-001",
            client = "ABC",
            consultant = "XYZ",
            location = "Riyadh",
            preparedBy = "A. Ismail",
            revision = "02",
            status = "For Review",
            documentNumber = "WPS-DOC-001",
            codes = "NFPA 20, NFPA 12",
            codeEdition = "2022",
        )
        projects.saveInfo(projectId, record)
        projects.setActiveProject(projectId)

        // ---- the active project reads back exactly what was written -------------
        val active = projects.activeProject()!!
        assertEquals(record, active, "the project record must survive a save and a reload")

        // ---- a calculation is saved against that project, with a frozen snapshot ----------
        val calculator = CalculatorRegistry.byIdOrThrow("pump-power")
        val inputs = mapOf(
            "q" to InputValue("q", 0.05, "m3h"),
            "h" to InputValue("h", 40.0, "m"),
            "eta" to InputValue("eta", 0.78, "pct"),
            "rho" to InputValue("rho", 1000.0, "kgm3"),
        )
        val output = calculator.run(inputs)
        history.add(
            calculatorId = "pump-power",
            title = "Fire pump power",
            timestamp = 1_726_200_000_001L,
            inputsJson = Snapshots.encodeInputs(inputs),
            resultsJson = Snapshots.encodeResults(output),
            projectId = projectId,
            projectSnapshot = record.toJson(),
            calculationNumber = "FP-CALC-004",
            revision = record.revision,
            status = record.status,
        )

        // ---- the register sees it, with its status ----------
        val rows = history.byProject(projectId)
        assertEquals(1, rows.size, "the saved calculation belongs to the project")
        assertEquals("FP-CALC-004", rows.first().calculation_number)
        assertEquals(listOf("For Review" to 1L), history.countsByStatus(projectId))

        // ---- and the sheet, the register and the package build from it ----------
        val labels = ReportLabels.ENGLISH
        val meta = ReportMeta.build(active, labels, ReportMeta.Fallback(), "2026-09-16").toMap()
        assertEquals("WPS-001", meta[labels.reportNo])
        assertEquals("ABC", meta[labels.client])
        assertEquals("NFPA 20, NFPA 12 - 2022", meta[labels.code])

        val entries = rows.map {
            ProjectRegister.Entry(
                calculationNumber = it.calculation_number.orEmpty(),
                calculatorName = it.title,
                revision = it.revision.orEmpty(),
                status = it.status.orEmpty(),
                preparedBy = record.preparedBy,
                date = ProjectPackage.dateOf(it.timestamp),
            )
        }
        val registerRows = ProjectRegister.build(active, entries, labels, "2026-09-16")
            .filterIsInstance<ReportBlock.TableRow>()
            .map { it.cells }
        assertEquals(listOf("FP-CALC-004", "Fire pump power", "02", "For Review", "A. Ismail", "2024-09-13"), registerRows[1])

        val packageBlocks = ProjectPackage.build(
            project = active,
            records = rows.map {
                ProjectPackage.Record(
                    calculationNumber = it.calculation_number.orEmpty(),
                    calculatorId = it.calculator_id,
                    title = it.title,
                    revision = it.revision.orEmpty(),
                    status = it.status.orEmpty(),
                    timestamp = it.timestamp,
                    inputsJson = it.inputs_json,
                    resultsJson = it.results_json,
                )
            },
            labels = labels,
            date = "2026-09-16",
            decodeInputs = { Snapshots.decodeInputs(it) },
            decodeOutput = { Snapshots.decodeResults(it) },
        )
        assertTrue(
            packageBlocks.none { it is ReportBlock.Warning },
            "the stored calculation must replay, not warn",
        )
        assertTrue(
            packageBlocks.count { it is ReportBlock.PageBreak } == 2,
            "the register plus one calculation means two page breaks",
        )

        // ---- and the checks are happy with a complete, restored record ----------
        val issues = ReportQa.check(
            project = active,
            calculationNumber = rows.first().calculation_number,
            revision = rows.first().revision,
            status = rows.first().status,
            assumedInputWarnings = emptyList(),
            isSaved = true,
        )
        assertTrue(issues.isEmpty(), "a complete saved calculation must raise no issue: $issues")

        // ---- the snapshot is what an old calculation prints, not today's project ----------
        val renamed = record.copy(name = "Renamed later", client = "Another client", revision = "05")
        projects.saveInfo(projectId, renamed)
        val restored = ReportMeta.projectFor(rows.first().project_snapshot, projects.activeProject())!!
        assertEquals("Wastewater Pump Station 01", restored.name)
        assertEquals("ABC", restored.client)
        assertEquals("02", restored.revision)
    }
}
