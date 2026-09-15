package com.mechforge.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mechforge.app.data.BuiltInDatasets
import com.mechforge.app.data.ReferencesRepository
import com.mechforge.db.MechForgeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Reference library: CSV import, built-in seeding and deletion (README v2 16/17/18). */
class ReferencesImportTest {

    private fun freshDb(): MechForgeDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MechForgeDatabase.Schema.create(driver)
        return MechForgeDatabase(driver)
    }

    @Test
    fun importsCsvWithHeaderQuotesAndPartialRows() {
        val db = freshDb()
        val repo = ReferencesRepository(db)
        val csv = listOf(
            "key,value,unit,notes",
            "Carbon steel,7850,kg/m3,\"typical, unfactored\"",
            "Motor rating 15 kW,15,kW,",
            "bare value,42",
        ).joinToString("\n")

        val result = repo.importCsv(
            "My data", "Materials", "Vendor catalogue 2026", "User-licensed",
            "Imported by the user", csv, 1_000L,
        )

        assertTrue(result.ok, "unexpected errors: ${result.errors}")
        assertEquals(3, result.rowsImported)

        val datasets = db.referenceDatasetsQueries.selectAllDatasets().executeAsList()
        assertEquals(1, datasets.size)
        assertEquals("My data", datasets.first().name)
        assertEquals(3L, datasets.first().row_count)

        val rows = repo.rows(datasets.first().id)
        assertEquals(3, rows.size)
        assertEquals("Carbon steel", rows[0].key)
        assertEquals("7850", rows[0].value)
        assertEquals("kg/m3", rows[0].unit)
        assertEquals("typical, unfactored", rows[0].notes)
        assertEquals("kW", rows[1].unit)
        assertEquals("42", rows[2].value)
        assertEquals("", rows[2].unit)
    }

    @Test
    fun rejectsUnusableCsv() {
        val db = freshDb()
        val repo = ReferencesRepository(db)
        val result = repo.importCsv("Empty", "Test", "n/a", "n/a", "n/a", "key,value\n", 1L)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("no usable rows") })
        assertEquals(0L, db.referenceDatasetsQueries.countDatasets().executeAsOne())
    }

    @Test
    fun builtInSeedingIsIdempotent() {
        val db = freshDb()
        val repo = ReferencesRepository(db)

        repo.seedBuiltIn(1_000L)
        val afterFirst = db.referenceDatasetsQueries.selectAllDatasets().executeAsList()
        assertEquals(BuiltInDatasets.all.size, afterFirst.size)

        repo.seedBuiltIn(2_000L)
        val afterSecond = db.referenceDatasetsQueries.selectAllDatasets().executeAsList()
        assertEquals(BuiltInDatasets.all.size, afterSecond.size, "seeding must not duplicate datasets")

        val expectedRows = BuiltInDatasets.all.sumOf { it.rows.size }.toLong()
        val actualRows = afterSecond.sumOf { repo.rowCount(it.id) }
        assertEquals(expectedRows, actualRows)

        // every built-in dataset carries its source and licence metadata
        for (dataset in afterSecond) {
            assertTrue(dataset.source.isNotBlank(), "missing source on ${dataset.name}")
            assertTrue(dataset.license_type.isNotBlank(), "missing licence on ${dataset.name}")
        }
    }

    @Test
    fun importsJsonInArrayAndObjectForm() {
        val db = freshDb()
        val repo = ReferencesRepository(db)

        val arrayJson = """[{"key":"Carbon steel","value":"7850","unit":"kg/m3"},""" +
            """{"name":"Air (20 C)","value":"1.204","unit":"kg/m3","notes":"standard air"}]"""
        val first = repo.importJson("From JSON", "Materials", "Vendor", "User-licensed", arrayJson, 1_000L)
        assertTrue(first.ok, first.errors.toString())
        assertEquals(2, first.rowsImported)

        val objectJson = """{"name":"Steel roughness","category":"Piping","source":"Catalogue",""" +
            """"license":"User-licensed","rows":[{"key":"Commercial steel","value":"0.045","unit":"mm"}]}"""
        val second = repo.importJson("fallback", "Test", "Vendor", "n/a", objectJson, 2_000L)
        assertTrue(second.ok, second.errors.toString())

        val byName = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().associateBy { it.name }
        assertEquals("From JSON", byName.keys.firstOrNull { it == "From JSON" })
        val arrayDataset = byName.getValue("From JSON")
        assertEquals(2L, arrayDataset.row_count)
        val rows = repo.rows(arrayDataset.id)
        assertEquals("Carbon steel", rows[0].key)
        assertEquals("Air (20 C)", rows[1].key)
        assertEquals("standard air", rows[1].notes)

        val objectDataset = byName.getValue("Steel roughness")
        assertEquals("Piping", objectDataset.category)
        assertEquals("Catalogue", objectDataset.source)
        assertEquals(1L, objectDataset.row_count)
        assertEquals("Commercial steel", repo.rows(objectDataset.id).first().key)
    }

    @Test
    fun rejectsInvalidJson() {
        val db = freshDb()
        val repo = ReferencesRepository(db)
        val result = repo.importJson("Broken", "Test", "n/a", "n/a", "{ not json ", 1L)
        assertFalse(result.ok)
        assertEquals(0L, db.referenceDatasetsQueries.countDatasets().executeAsOne())
    }

    /**
     * Regression: every dataset must own its own rows. The first implementation resolved the
     * new dataset id by list order while all built-in datasets shared one timestamp, so every
     * row was attached to whichever dataset happened to sort first.
     */
    @Test
    fun eachBuiltInDatasetOwnsItsOwnRows() {
        val db = freshDb()
        val repo = ReferencesRepository(db)
        repo.seedBuiltIn(1_000L)

        val byName = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().associateBy { it.name }
        for (expected in BuiltInDatasets.all) {
            val dataset = byName.getValue(expected.name)
            assertEquals(expected.rows.size.toLong(), dataset.row_count, "row_count mismatch for ${expected.name}")
            val rows = repo.rows(dataset.id)
            assertEquals(expected.rows.size, rows.size, "rows mismatch for ${expected.name}")
            assertEquals(expected.rows.first()[0], rows.first().key)
            assertEquals(expected.rows.last()[0], rows.last().key)
        }

        val densities = repo.rows(byName.getValue("Material densities (typical)").id)
        assertTrue(densities.any { it.key == "Carbon steel" && it.value == "7850" })
        val motors = repo.rows(byName.getValue("IEC standard motor ratings").id)
        assertTrue(motors.any { it.key == "Motor rating 15 kW" && it.value == "15" })
        assertFalse(motors.any { it.key == "Carbon steel" }, "density rows must not leak into the motor dataset")
    }

    /**
     * The earlier build attached every row to the first dataset. Seeding must detect such a
     * dataset (row count / first key differ from the built-in definition) and repair it.
     */
    @Test
    fun seedingRepairsADatasetHoldingWrongRows() {
        val db = freshDb()
        val repo = ReferencesRepository(db)

        db.referenceDatasetsQueries.insertDataset(
            "Material densities (typical)", "Materials", "old", "generic", "old", 1L, 2L,
        )
        val brokenId = db.referenceDatasetsQueries.lastInsertRowId().executeAsOne()
        db.referenceRowsQueries.insertRow(brokenId, "Motor rating 15 kW", "15", "kW", "")
        db.referenceRowsQueries.insertRow(brokenId, "Motor rating 22 kW", "22", "kW", "")

        repo.seedBuiltIn(1_000L)

        val repaired = db.referenceDatasetsQueries.selectAllDatasets().executeAsList()
            .first { it.name == "Material densities (typical)" }
        val rows = repo.rows(repaired.id)
        val expected = BuiltInDatasets.all.first { it.name == "Material densities (typical)" }
        assertEquals(expected.rows.size, rows.size)
        assertTrue(rows.any { it.key == "Carbon steel" && it.value == "7850" }, "density rows must be restored")
        assertFalse(rows.any { it.key.startsWith("Motor rating") }, "wrong rows must be gone")
        assertEquals(BuiltInDatasets.all.size, db.referenceDatasetsQueries.countDatasets().executeAsOne().toInt())
    }

    @Test
    fun deleteRemovesDatasetAndItsRows() {
        val db = freshDb()
        val repo = ReferencesRepository(db)
        repo.importCsv("Temp", "Test", "n/a", "n/a", "n/a", "a,1,mm\nb,2,mm", 1L)
        val id = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().first().id
        assertEquals(2L, repo.rowCount(id))

        repo.deleteDataset(id)

        assertEquals(0L, db.referenceDatasetsQueries.countDatasets().executeAsOne())
        assertEquals(0, repo.rows(id).size, "rows of a deleted dataset must be gone")
    }
}
