package com.mechforge.app.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mechforge.db.Calculation_history
import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

data class RecentCalculator(val calculatorId: String, val lastUsed: Long)

class HistoryRepository(private val db: MechForgeDatabase) {

    fun all(): Flow<List<Calculation_history>> =
        db.historyQueries.selectAllHistory().asFlow().mapToList(Dispatchers.IO)

    fun add(calculatorId: String, title: String, timestamp: Long, inputsJson: String, resultsJson: String) {
        db.historyQueries.insertHistory(calculatorId, title, timestamp, inputsJson, resultsJson)
    }

    fun byId(id: Long): Calculation_history? =
        db.historyQueries.getHistoryById(id).executeAsOneOrNull()

    fun rename(id: Long, title: String) = db.historyQueries.updateHistoryTitle(title, id)

    fun duplicate(id: Long, timestamp: Long) = db.historyQueries.duplicateHistory(timestamp, id)

    fun delete(id: Long) = db.historyQueries.deleteHistory(id)

    fun recentCalculators(limit: Long = 6L): List<RecentCalculator> =
        db.historyQueries.recentCalculators(limit).executeAsList()
            .map { RecentCalculator(it.calculator_id, it.last_used ?: 0L) }
}

class FavoritesRepository(private val db: MechForgeDatabase) {

    fun all(): Flow<List<com.mechforge.db.Favorites>> =
        db.favoritesQueries.selectAllFavorites().asFlow().mapToList(Dispatchers.IO)

    fun isFavorite(calculatorId: String): Boolean =
        db.favoritesQueries.getFavorite(calculatorId).executeAsOneOrNull() != null

    fun toggle(calculatorId: String, timestamp: Long) {
        if (isFavorite(calculatorId)) {
            db.favoritesQueries.deleteFavorite(calculatorId)
        } else {
            db.favoritesQueries.upsertFavorite(calculatorId, timestamp)
        }
    }
}

class ProjectsRepository(private val db: MechForgeDatabase) {

    fun all(): Flow<List<com.mechforge.db.SelectAllProjects>> =
        db.projectsQueries.selectAllProjects().asFlow()
            .mapToList(Dispatchers.IO)

    fun create(name: String, description: String, timestamp: Long) =
        db.projectsQueries.insertProject(name, description, timestamp)

    fun rename(id: Long, name: String) = db.projectsQueries.updateProjectName(name, id)

    fun delete(id: Long) = db.projectsQueries.deleteProject(id)
}
