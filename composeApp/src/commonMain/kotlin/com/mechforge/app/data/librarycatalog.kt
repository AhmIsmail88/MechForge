package com.mechforge.app.data

/**
 * Maps an [com.mechforge.core.engine.InputSpec.libraryKey] to the built-in reference
 * dataset that can fill that input (README v2 17: the library should feed the
 * calculators, not just be browsable).
 */
object LibraryCatalog {

    const val ROUGHNESS = "roughness"
    const val DENSITY = "density"

    val datasetNames: Map<String, String> = mapOf(
        ROUGHNESS to "Pipe absolute roughness (typical)",
        DENSITY to "Material densities (typical)",
    )

    /**
     * Reference rows carry plain-ASCII units (kg/m3, m/s2). Calculators use the typeset
     * symbols (kg/m³, m/s²). This maps the former onto the latter so a library value can
     * also select the matching unit.
     */
    fun normaliseUnitSymbol(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed
            .replace("3", "\u00B3")
            .replace("2", "\u00B2")
            .replace(".", "\u00B7")
            .replace("(", "(")
    }
}
