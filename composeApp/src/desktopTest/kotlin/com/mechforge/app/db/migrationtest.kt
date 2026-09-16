package com.mechforge.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mechforge.db.MechForgeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * README v2 §7.1 acceptance criterion 3: database migrations are tested by
 * upgrading a real v1 fixture database (with data) to the current schema.
 */
class MigrationTest {

    @Test
    fun v1ToCurrentPreservesDataAndAddsReferencesTable() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Build a real v1 database from the fixture (schema + data rows).
        val v1Sql = javaClass.classLoader.getResource("db/v1_schema.sql")!!.readText()
        for (statement in v1Sql.split(';')) {
            val sql = statement.trim()
            if (sql.isNotEmpty()) driver.execute(null, sql, 0)
        }

        // Upgrade v1 -> current.
        MechForgeDatabase.Schema.migrate(driver, 1L, MechForgeDatabase.Schema.version)

        val db = MechForgeDatabase(driver)

        // Pre-migration data survived.
        val history = db.historyQueries.selectAllHistory().executeAsList()
        assertEquals(1, history.size)
        assertEquals("Pump Power run", history.first().title)

        assertEquals(1, db.favoritesQueries.selectAllFavorites().executeAsList().size)
        assertEquals("system", db.settingsQueries.getSetting("theme").executeAsOne().value_)
        assertEquals(1, db.projectsQueries.selectAllProjects().executeAsList().size)
        assertEquals(1, db.calculatorsQueries.countCalculators().executeAsOne())

        // The v2 table is usable.
        assertEquals(0L, db.referencesQueries.countAllReferences().executeAsOne())
        db.referencesQueries.insertEngineeringReference(
            "Water properties (IAPWS-style correlations)",
            "Fluid properties",
            "Computed from published formulations — no copyrighted tables embedded",
            "n/a",
            "formula-only",
            "Implemented from public-domain formulations per README v2 §18",
            1726200000000L,
            "engine",
        )
        assertEquals(1L, db.referencesQueries.countAllReferences().executeAsOne())
        assertTrue(db.referencesQueries.selectAllReferences().executeAsList().isNotEmpty())

        // v3 -> v4: the reference library tables exist on the migrated database and are usable.
        assertEquals(0L, db.referenceDatasetsQueries.countDatasets().executeAsOne())
        db.referenceDatasetsQueries.insertDataset(
            "Post-migration dataset", "Test", "unit test", "n/a", "test fixture",
            1726200000000L, 1L,
        )
        val datasetId = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().first().id
        db.referenceRowsQueries.insertRow(datasetId, "row key", "42", "mm", "from the migration test")
        assertEquals(1L, db.referenceDatasetsQueries.countDatasets().executeAsOne())
        assertEquals(1L, db.referenceRowsQueries.countRowsByDataset(datasetId).executeAsOne())
        assertEquals("42", db.referenceRowsQueries.selectRowsByDataset(datasetId).executeAsList().first().value_)

        // v4 -> v5: the project owns the engineering record and a saved calculation keeps the
        // snapshot (engineering audit sections 4, 5, 11). Rows that predate the migration survive
        // with empty (null) new columns.
        val project = db.projectsQueries.selectAllProjects().executeAsList().first()
        assertTrue(
            project.client == null && project.revision == null && project.status == null,
            "the new project columns must start empty on an upgraded database",
        )
        db.projectsQueries.updateProjectInfo(
            name = "Wastewater Pump Station 01",
            description = "audit stage 1",
            project_number = "WPS-001",
            project_code = null,
            project_type = "Fire protection",
            location = "Riyadh",
            country = "Saudi Arabia",
            client = "ABC",
            consultant = "XYZ",
            contractor = null,
            end_user = null,
            prepared_by = "A. Ismail",
            checked_by = null,
            approved_by = null,
            discipline = "Fire Protection",
            revision = "02",
            revision_date = null,
            status = "For Review",
            document_number = "WPS-DOC-001",
            codes = "NFPA 20",
            code_edition = "2022",
            design_conditions = "Ambient 45 C",
            notes = null,
            id = project.id,
        )
        val reloaded = db.projectsQueries.selectProjectById(project.id).executeAsOne()
        assertEquals("WPS-001", reloaded.project_number)
        assertEquals("ABC", reloaded.client)
        assertEquals("02", reloaded.revision)
        assertEquals("NFPA 20", reloaded.codes)

        db.savedcalculationsQueries.insertSaved(
            calculator_id = "pump-power",
            name = "Fire pump power",
            timestamp = 1726200000000L,
            inputs_json = "{}",
            results_json = "{}",
            project_id = project.id,
            project_snapshot = "{\"name\":\"Wastewater Pump Station 01\",\"revision\":\"02\"}",
            calculation_number = "FP-CALC-004",
            revision = "02",
            status = "Draft",
            steps_json = "[]",
            warnings_json = "[]",
        )
        val saved = db.savedcalculationsQueries.selectAllSaved().executeAsList().first()
        assertEquals("FP-CALC-004", saved.calculation_number)
        assertTrue(saved.project_snapshot!!.contains("Wastewater Pump Station 01"))
        assertEquals(project.id, saved.project_id)

        driver.close()
    }
}
