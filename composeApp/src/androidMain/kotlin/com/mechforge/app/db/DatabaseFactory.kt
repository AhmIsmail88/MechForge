package com.mechforge.app.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mechforge.db.MechForgeDatabase

/**
 * Android SQLite database factory. The database lives in the app's private storage
 * (`mechforge.db`), so it is fully offline and removed on uninstall.
 *
 * [AndroidSqliteDriver] creates the schema on a fresh install and applies
 * SQLDelight's generated migration chain on upgrade (README v2 §21), so the table
 * and JSON contract is identical to the desktop target.
 */
object DatabaseFactory {

    private const val DATABASE_NAME = "mechforge.db"

    fun create(context: Context): MechForgeDatabase =
        MechForgeDatabase(
            AndroidSqliteDriver(
                schema = MechForgeDatabase.Schema,
                context = context.applicationContext,
                name = DATABASE_NAME,
            ),
        )
}
