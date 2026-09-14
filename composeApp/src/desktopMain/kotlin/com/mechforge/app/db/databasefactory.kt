package com.mechforge.app.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mechforge.db.MechForgeDatabase
import java.io.File

/**
 * Desktop SQLite database factory. The DB lives in ~/.mechforge/mechforge.db.
 * Schema versioning per README v2 §21: fresh installs are created at the current
 * schema version; existing databases are migrated forward with SQLDelight's
 * generated migration chain (v1 -> v2 exercised by MigrationTest).
 */
object DatabaseFactory {

    fun create(): MechForgeDatabase {
        val dir = File(System.getProperty("user.home"), ".mechforge").apply { mkdirs() }
        return create(File(dir, "mechforge.db"))
    }

    fun create(file: File): MechForgeDatabase {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath.replace('\\', '/')}")
        prepareSchema(driver)
        return MechForgeDatabase(driver)
    }

    private fun prepareSchema(driver: SqlDriver) {
        val schema = MechForgeDatabase.Schema
        val currentVersion = readUserVersion(driver)
        val tablesExist = tableCount(driver) > 0

        when {
            !tablesExist && currentVersion == 0L -> {
                schema.create(driver)
                setUserVersion(driver, schema.version)
            }
            currentVersion in 1 until schema.version -> {
                schema.migrate(driver, currentVersion, schema.version)
                setUserVersion(driver, schema.version)
            }
        }
    }

    private fun readUserVersion(driver: SqlDriver): Long =
        driver.executeQuery(
            null,
            "PRAGMA user_version",
            { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            0,
        ).value

    private fun tableCount(driver: SqlDriver): Long =
        driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'",
            { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            0,
        ).value

    private fun setUserVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version", 0)
    }
}
