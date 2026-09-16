package com.mechforge.app.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.mechforge.db.Calculation_history
import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RecentCalculator(val calculatorId: String, val lastUsed: Long)

class HistoryRepository(private val db: MechForgeDatabase) {

    fun all(): Flow<List<Calculation_history>> =
        db.historyQueries.selectAllHistory().asFlow().mapToList(Dispatchers.IO)

    /**
     * Saves one calculation. When a project is active, [projectSnapshot] freezes its data with
     * the record (engineering audit section 5.1), so a project revision made later cannot
     * rewrite a calculation that was already issued.
     */
    fun add(
        calculatorId: String,
        title: String,
        timestamp: Long,
        inputsJson: String,
        resultsJson: String,
        projectId: Long? = null,
        projectSnapshot: String? = null,
        calculationNumber: String? = null,
        revision: String? = null,
        status: String? = null,
    ) {
        db.historyQueries.insertHistoryFull(
            calculator_id = calculatorId,
            title = title,
            timestamp = timestamp,
            inputs_json = inputsJson,
            results_json = resultsJson,
            project_id = projectId,
            project_snapshot = projectSnapshot,
            calculation_number = calculationNumber,
            revision = revision,
            status = status,
        )
    }

    /** Every calculation saved under one project, newest first (the project register). */
    fun byProject(projectId: Long): List<Calculation_history> =
        db.historyQueries.selectHistoryByProject(projectId).executeAsList()

    /** How many calculations of a project stand at each document status. */
    fun countsByStatus(projectId: Long): List<Pair<String, Long>> =
        db.historyQueries.countHistoryByStatus(projectId).executeAsList()
            .map { (it.status ?: "") to it.total }

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

    /**
     * The complete engineering record of one project (engineering audit sections 4, 5, 11).
     * The project - not Settings - owns the data a calculation report prints.
     */
    fun info(id: Long): ProjectInfo? =
        db.projectsQueries.selectProjectById(id).executeAsOneOrNull()?.let { row ->
            ProjectInfo(
                name = row.name,
                description = row.description,
                projectNumber = row.project_number.orEmpty(),
                projectCode = row.project_code.orEmpty(),
                projectType = row.project_type.orEmpty(),
                location = row.location.orEmpty(),
                country = row.country.orEmpty(),
                client = row.client.orEmpty(),
                consultant = row.consultant.orEmpty(),
                contractor = row.contractor.orEmpty(),
                endUser = row.end_user.orEmpty(),
                preparedBy = row.prepared_by.orEmpty(),
                checkedBy = row.checked_by.orEmpty(),
                approvedBy = row.approved_by.orEmpty(),
                discipline = row.discipline.orEmpty(),
                revision = row.revision.orEmpty(),
                revisionDate = row.revision_date,
                status = row.status.orEmpty(),
                documentNumber = row.document_number.orEmpty(),
                codes = row.codes.orEmpty(),
                codeEdition = row.code_edition.orEmpty(),
                designConditions = row.design_conditions.orEmpty(),
                notes = row.notes.orEmpty(),
            )
        }

    /**
     * Writes the engineering record back. A blank field is stored as NULL so the database keeps
     * "not entered" distinct from "entered as empty", and the report can skip both.
     */
    fun saveInfo(id: Long, info: ProjectInfo) {
        db.projectsQueries.updateProjectInfo(
            name = info.name,
            description = info.description,
            project_number = info.projectNumber.blankToNull(),
            project_code = info.projectCode.blankToNull(),
            project_type = info.projectType.blankToNull(),
            location = info.location.blankToNull(),
            country = info.country.blankToNull(),
            client = info.client.blankToNull(),
            consultant = info.consultant.blankToNull(),
            contractor = info.contractor.blankToNull(),
            end_user = info.endUser.blankToNull(),
            prepared_by = info.preparedBy.blankToNull(),
            checked_by = info.checkedBy.blankToNull(),
            approved_by = info.approvedBy.blankToNull(),
            discipline = info.discipline.blankToNull(),
            revision = info.revision.blankToNull(),
            revision_date = info.revisionDate,
            status = info.status.blankToNull(),
            document_number = info.documentNumber.blankToNull(),
            codes = info.codes.blankToNull(),
            code_edition = info.codeEdition.blankToNull(),
            design_conditions = info.designConditions.blankToNull(),
            notes = info.notes.blankToNull(),
            id = id,
        )
    }

    /** The project a new calculation and its report belong to until the engineer changes it. */
    fun activeProjectId(): Long? =
        db.settingsQueries.getSetting(KEY_ACTIVE_PROJECT).executeAsOneOrNull()
            ?.value_?.toLongOrNull()

    /** The active project id as a stream, so the project list can mark it live. */
    fun activeProjectIdFlow(): Flow<Long?> =
        db.settingsQueries.getSetting(KEY_ACTIVE_PROJECT).asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.value_?.toLongOrNull() }

    fun setActiveProject(id: Long) =
        db.settingsQueries.upsertSetting(KEY_ACTIVE_PROJECT, id.toString())

    /**
     * The active project, read back in full. Falls back to the most recent project when no
     * project was chosen yet, so a single-project installation needs no extra step.
     */
    fun activeProject(): ProjectInfo? {
        val chosen = activeProjectId()?.let { info(it) }
        if (chosen != null) return chosen
        val newest = db.projectsQueries.selectAllProjects().executeAsList().firstOrNull() ?: return null
        return info(newest.id)
    }

    private fun String.blankToNull(): String? = if (isBlank()) null else this

    private companion object {
        const val KEY_ACTIVE_PROJECT = "active_project_id"
    }
}
