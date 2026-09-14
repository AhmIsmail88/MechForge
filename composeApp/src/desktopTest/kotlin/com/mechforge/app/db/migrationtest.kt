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

        driver.close()
    }
}
