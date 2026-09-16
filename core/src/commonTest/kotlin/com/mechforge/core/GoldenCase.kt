package com.mechforge.core

/**
 * Golden test data (audit P0-3).
 *
 * [baseValue] is the input in the family's SI base unit, exactly as the engine stores it;
 * [displayUnitId] is the unit it was entered in. [expected] is the value the independent
 * Python model derives for each result id, in the unit that result declares.
 *
 * The expectations live in the generated file `GoldenCases.kt` and never come from the
 * Kotlin engine, so the comparison is against a separate implementation of the equations.
 */
data class GoldenInput(
    val inputId: String,
    val baseValue: Double,
    val displayUnitId: String,
)

data class GoldenCase(
    val calculatorId: String,
    val scenario: String,
    val inputs: List<GoldenInput>,
    val expected: Map<String, Double>,
    val relativeTolerance: Double,
)