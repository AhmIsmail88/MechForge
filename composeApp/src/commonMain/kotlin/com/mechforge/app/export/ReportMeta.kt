package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo

/**
 * The project block printed at the top of a calculation report (engineering audit section 11).
 *
 * The project owns this data. The values kept in Settings are only the fallback used while a
 * project has not been filled in, so an installation created before the project area existed
 * still prints a letterhead and the export never loses information.
 */
object ReportMeta {

    /** Values held in Settings, used only when the project record is still blank. */
    data class Fallback(
        val project: String = "",
        val client: String = "",
        val engineer: String = "",
        val location: String = "",
        val reportNo: String = "",
        val revision: String = "",
        val code: String = "",
    )

    fun build(
        project: ProjectInfo?,
        labels: ReportLabels,
        fallback: Fallback,
        date: String,
    ): List<Pair<String, String>> {
        // A project that is only named still owns its name: the report prints it and takes the
        // remaining fields from Settings until the engineer fills the project in.
        if (project == null || project.isEmpty) {
            return fromFallback(labels, fallback, date, project?.name.orEmpty())
        }

        val code = listOf(project.codes, project.codeEdition)
            .filter { it.isNotBlank() }
            .joinToString(" - ")

        return listOfNotNull(
            project.name.takeIf { it.isNotBlank() }?.let { labels.project to it },
            project.projectNumber.takeIf { it.isNotBlank() }?.let { labels.reportNo to it },
            project.client.takeIf { it.isNotBlank() }?.let { labels.client to it },
            project.consultant.takeIf { it.isNotBlank() }?.let { labels.consultant to it },
            project.contractor.takeIf { it.isNotBlank() }?.let { labels.contractor to it },
            project.location.takeIf { it.isNotBlank() }?.let { labels.location to it },
            code.takeIf { it.isNotBlank() }?.let { labels.code to it },
            project.revision.takeIf { it.isNotBlank() }?.let { labels.revision to it },
            project.status.takeIf { it.isNotBlank() }?.let { labels.status to it },
            project.preparedBy.takeIf { it.isNotBlank() }?.let { labels.engineer to it },
            project.documentNumber.takeIf { it.isNotBlank() }?.let { labels.documentNo to it },
            labels.date to date,
        )
    }

    private fun fromFallback(
        labels: ReportLabels,
        fallback: Fallback,
        date: String,
        projectName: String = "",
    ): List<Pair<String, String>> = listOfNotNull(
        projectName.ifBlank { fallback.project }.takeIf { it.isNotBlank() }?.let { labels.project to it },
        fallback.client.takeIf { it.isNotBlank() }?.let { labels.client to it },
        fallback.engineer.takeIf { it.isNotBlank() }?.let { labels.engineer to it },
        fallback.location.takeIf { it.isNotBlank() }?.let { labels.location to it },
        fallback.reportNo.takeIf { it.isNotBlank() }?.let { labels.reportNo to it },
        fallback.revision.takeIf { it.isNotBlank() }?.let { labels.revision to it },
        labels.date to date,
        fallback.code.takeIf { it.isNotBlank() }?.let { labels.code to it },
    )
}
