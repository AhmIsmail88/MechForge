package com.mechforge.app.export

/**
 * The project dashboard numbers (engineering audit section 7.1).
 *
 * Pure so the screen stays thin and the arithmetic is testable: the caller supplies how to look a
 * calculator's discipline up, and this object does the grouping and the ordering.
 */
object ProjectSummary {

    /** One discipline (a calculator category) and how many calculations of the project it holds. */
    data class Discipline(val name: String, val count: Int)

    /** One line of the recent-calculation list. */
    data class Recent(val label: String, val title: String, val date: String)

    /**
     * Calculations per discipline, largest first and then alphabetically, so the busiest part of a
     * project is the first thing the engineer sees. Records whose calculator is unknown are
     * collected under [unknownLabel] instead of being dropped.
     */
    fun disciplines(
        calculatorIds: List<String>,
        disciplineOf: (String) -> String?,
        unknownLabel: String,
    ): List<Discipline> =
        calculatorIds
            .groupingBy { disciplineOf(it)?.takeIf { name -> name.isNotBlank() } ?: unknownLabel }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { Discipline(it.key, it.value) }

    /**
     * The newest [limit] calculations as display lines. [records] is expected newest-first, which
     * is the order the register and the project list already use.
     */
    fun recent(
        records: List<Triple<String, String, String>>,
        limit: Int = 5,
    ): List<Recent> =
        records.take(limit.coerceAtLeast(0)).map { (label, title, date) -> Recent(label, title, date) }

    /** Percent of the project's calculations that carry each status, for a compact read-out. */
    fun statusPercent(counts: List<Pair<String, Int>>): List<Pair<String, Int>> {
        val total = counts.sumOf { it.second }
        if (total <= 0) return emptyList()
        return counts.map { (status, count) -> status to (count * 100.0 / total).toInt() }
    }
}
