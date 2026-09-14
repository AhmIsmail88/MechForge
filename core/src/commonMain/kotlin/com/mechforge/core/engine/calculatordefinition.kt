package com.mechforge.core.engine

/**
 * Static metadata + input contract for one calculator. The definition carries no
 * math; the [Calculator] subclass owns the formula. This mirrors README §19/§38.
 */
data class CalculatorDefinition(
    val id: String,
    val name: String,
    val category: CalculatorCategory,
    val description: String,
    val formulaDisplay: String,
    val reference: String,
    val notes: String = "",
    val keywords: List<String> = emptyList(),
    val inputs: List<InputSpec>,
    val version: Int = 1,
)
