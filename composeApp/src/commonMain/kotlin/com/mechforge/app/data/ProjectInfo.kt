package com.mechforge.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything a calculation report needs about the project it belongs to (engineering audit
 * sections 4, 5 and 11).
 *
 * The project is engineering data, not an application preference: it is edited from the
 * project area and never from Settings. A saved calculation stores this object as a
 * snapshot, so revising the project later cannot rewrite a calculation that was already
 * issued.
 */
@Serializable
data class ProjectInfo(
    val name: String = "",
    val description: String = "",
    val projectNumber: String = "",
    val projectCode: String = "",
    val projectType: String = "",
    val location: String = "",
    val country: String = "",
    val client: String = "",
    val consultant: String = "",
    val contractor: String = "",
    val endUser: String = "",
    val preparedBy: String = "",
    val checkedBy: String = "",
    val approvedBy: String = "",
    val discipline: String = "",
    val revision: String = "",
    val revisionDate: Long? = null,
    val status: String = "",
    val documentNumber: String = "",
    val codes: String = "",
    val codeEdition: String = "",
    val designConditions: String = "",
    val notes: String = "",
) {
    /** True when nothing but the project name is known, so the report can fall back to Settings. */
    val isEmpty: Boolean
        get() = listOf(
            description, projectNumber, projectCode, projectType, location, country, client, consultant,
            contractor, endUser, preparedBy, checkedBy, approvedBy, discipline, revision,
            documentNumber, codes, codeEdition, designConditions, notes,
        ).all { it.isBlank() } && revisionDate == null

    /** The rows the report prints, in order, skipping everything that was left empty. */
    fun reportRows(): List<Pair<String, String>> = listOf(
        "Project" to name,
        "Description" to description,
        "Project No." to projectNumber,
        "Project Code" to projectCode,
        "Project Type" to projectType,
        "Location" to location,
        "Country" to country,
        "Client" to client,
        "Consultant" to consultant,
        "Contractor" to contractor,
        "End User" to endUser,
        "Prepared By" to preparedBy,
        "Checked By" to checkedBy,
        "Approved By" to approvedBy,
        "Discipline" to discipline,
        "Document No." to documentNumber,
        "Revision" to revision,
        "Status" to status,
        "Applicable Codes" to codes,
        "Code Edition" to codeEdition,
        "Design Conditions" to designConditions,
        "Notes" to notes,
    ).filter { it.second.isNotBlank() }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(text: String?): ProjectInfo? {
            if (text.isNullOrBlank()) return null
            return runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
        }
    }
}
