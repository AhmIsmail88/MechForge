package com.mechforge.app.export

/**
 * Structured content of one engineering calculation sheet, shared by both platforms.
 * A platform renderer only has to know how to draw these blocks, so the Android and
 * desktop PDF writers stay in sync.
 */
sealed interface ReportBlock {
    data class Heading(val text: String) : ReportBlock
    data class Paragraph(val text: String) : ReportBlock
    data class KeyValue(val label: String, val value: String) : ReportBlock
    data class TableRow(val cells: List<String>) : ReportBlock
    data object Divider : ReportBlock
    data class Warning(val text: String) : ReportBlock
}
