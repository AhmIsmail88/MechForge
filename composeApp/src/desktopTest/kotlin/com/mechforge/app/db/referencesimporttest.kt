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
