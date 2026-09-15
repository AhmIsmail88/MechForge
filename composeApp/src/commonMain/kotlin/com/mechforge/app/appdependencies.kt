package com.mechforge.app

import com.mechforge.app.data.FavoritesRepository
import com.mechforge.app.data.HistoryRepository
import com.mechforge.app.data.ProjectsRepository
import com.mechforge.app.data.ReferencesRepository
import com.mechforge.app.data.SettingsRepository
import com.mechforge.app.export.LogoStore
import com.mechforge.app.export.ReportExporter
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.units.Units
import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hand-rolled constructor injection (Decision 0002): one container built by the
 * platform entry point (desktop `main`, Android `MechForgeApplication`) and passed
 * explicitly to the shared UI. No DI framework.
 *
 * Everything except the SQLite [MechForgeDatabase] driver and the [ReportExporter]
 * is shared verbatim between the desktop and Android targets.
 */
class AppDependencies(
    val database: MechForgeDatabase,
    val exporter: ReportExporter,
    val logoStore: LogoStore,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val settings = SettingsRepository(database)
    val history = HistoryRepository(database)
    val favorites = FavoritesRepository(database)
    val projects = ProjectsRepository(database)
    val references = ReferencesRepository(database)

    init {
        seedCalculators()
        seedUnitsAndMetadata()
        seedReferenceLibrary()
    }

    /** Mirror the code registry into the calculators table (idempotent upserts). */
    private fun seedCalculators() {
        scope.launch(Dispatchers.IO) {
            database.calculatorsQueries.transaction {
                for (calc in CalculatorRegistry.all) {
                    val d = calc.def
                    database.calculatorsQueries.upsertCalculator(
                        d.id, d.name, d.category.name, d.description,
                        d.formulaDisplay, d.reference, d.version.toLong(), 1L,
                    )
                }
            }
        }
    }

    /**
     * Mirror the unit registry and record app metadata. Linear representation:
     * base = factor * value + offset (offset units such as °C/°F included).
     */
    private fun seedUnitsAndMetadata() {
        scope.launch(Dispatchers.IO) {
            database.unitsQueries.transaction {
                for (unit in Units.all) {
                    val offset = unit.toBase(0.0)
                    val factor = unit.toBase(1.0) - offset
                    val isBase = unit.family == UnitFamily.DIMENSIONLESS || unit.id == Units.defaultUnit(unit.family).id
                    database.unitsQueries.upsertUnit(
                        unit.id, unit.family.name, unit.symbol, factor, offset, if (isBase) 1L else 0L,
                    )
                }
            }
            database.metadataQueries.transaction {
                database.metadataQueries.upsertMetadata("schema_version", MechForgeDatabase.Schema.version.toString())
                database.metadataQueries.upsertMetadata("app_version", APP_VERSION)
                database.metadataQueries.upsertMetadata("calculator_count", CalculatorRegistry.all.size.toString())
            }
        }
    }

    /** Built-in generic reference datasets (idempotent by name). */
    private fun seedReferenceLibrary() {
        scope.launch(Dispatchers.IO) {
            references.seedBuiltIn(System.currentTimeMillis())
        }
    }

    companion object {
        const val APP_VERSION = "0.1.0"
    }
}
