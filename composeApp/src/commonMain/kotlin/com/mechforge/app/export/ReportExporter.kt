package com.mechforge.app.export

/**
 * Platform report-saving abstraction (README v2 §36).
 *
 * Desktop opens a native save dialog; Android writes the report into the app's own
 * files directory. Implementations are provided through the hand-rolled
 * [com.mechforge.app.AppDependencies] container (Decision 0002), so no platform
 * type leaks into the shared UI.
 */
interface ReportExporter {

    /**
     * Writes [content] under a file derived from [defaultName].
     *
     * @return the path written, or `null` if the user cancelled.
     */
    suspend fun save(defaultName: String, content: String): String?

    /**
     * Writes a paginated A4 PDF report built from the shared [ReportBlock] model.
     *
     * @return the path written, or 
ull when the user cancelled or rendering failed.
     */
    suspend fun savePdf(
        defaultName: String,
        title: String,
        meta: List<Pair<String, String>>,
        blocks: List<ReportBlock>,
    ): String?
}
