package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo

/**
 * Report quality checks (engineering audit section 12).
 *
 * A calculation result is not an approved design document: a sheet that leaves the office should
 * carry a project, a number, a revision and a status, and it should say which numbers were
 * assumed. These checks run before an export and report what is missing; they never block the
 * export, because the engineer decides what is good enough to issue.
 */
object ReportQa {

    enum class Severity { WARNING, INFO }

    data class Issue(val severity: Severity, val message: String)

    fun check(
        project: ProjectInfo?,
        calculationNumber: String?,
        revision: String?,
        status: String?,
        assumedInputWarnings: List<String>,
        isSaved: Boolean,
    ): List<Issue> {
        val issues = mutableListOf<Issue>()

        if (project == null) {
            issues += Issue(
                Severity.WARNING,
                "No project is selected: the sheet will print without project data.",
            )
        } else if (project.isEmpty) {
            issues += Issue(
                Severity.WARNING,
                "The project record is still blank: fill it in from the project area so the sheet " +
                    "identifies what it belongs to.",
            )
        }

        if (!isSaved || calculationNumber.isNullOrBlank()) {
            issues += Issue(
                Severity.INFO,
                "The sheet is not linked to a saved calculation yet: save it so it gets a number " +
                    "and appears in the project register.",
            )
        }

        if (revision.isNullOrBlank()) {
            issues += Issue(Severity.INFO, "The sheet carries no revision.")
        }

        if (status.isNullOrBlank()) {
            issues += Issue(
                Severity.INFO,
                "The document status is empty: 'Draft', 'For Review', 'For Approval', 'Approved' " +
                    "or 'As Built' record where the sheet stands.",
            )
        }

        if (assumedInputWarnings.isNotEmpty()) {
            issues += Issue(
                Severity.INFO,
                "${assumedInputWarnings.size} assumed value(s) were used - confirm them against " +
                    "the project data or the manufacturer before issuing.",
            )
        }

        return issues
    }

    /** The issues as report lines, grouped so a reader sees the blockers first. */
    fun summarise(issues: List<Issue>): List<String> = issues
        .sortedBy { it.severity.ordinal }
        .map { "${it.severity.name}: ${it.message}" }
}
