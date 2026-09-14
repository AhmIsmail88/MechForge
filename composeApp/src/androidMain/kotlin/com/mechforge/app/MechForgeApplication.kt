package com.mechforge.app

import android.app.Application
import com.mechforge.app.db.DatabaseFactory
import com.mechforge.app.export.AndroidReportExporter

/**
 * Process-wide dependency container for the Android shell (README v2 §41, Phase 1).
 *
 * Holding [AppDependencies] here — rather than in the Activity — keeps one SQLite
 * driver and one seeding scope for the whole process, exactly like the desktop
 * entry point builds it once in `main()`.
 */
class MechForgeApplication : Application() {

    val dependencies: AppDependencies by lazy {
        AppDependencies(
            database = DatabaseFactory.create(this),
            exporter = AndroidReportExporter(this),
        )
    }
}
