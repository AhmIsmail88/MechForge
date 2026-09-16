package com.mechforge.core.engine

import kotlinx.serialization.Serializable

@Serializable
data class ResultValue(
    val id: String,
    val label: String,
    val value: Double,
    val unitId: String,
    val isPrimary: Boolean = false,
    val isRecommended: Boolean = false,
)

@Serializable
data class CalcOutput(
    val results: List<ResultValue> = emptyList(),
    val steps: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    /**
     * The same steps and warnings in Arabic, when the calculator provides them.
     *
     * They are additive and optional: a calculator without Arabic text leaves them empty and the
     * interface falls back to the English lists line for line, so the engine never depends on the
     * language and no number is ever recomputed for a translation. `ArabicOutputParityTest` checks
     * that the two lists agree in length and in every number they print.
     */
    val stepsAr: List<String> = emptyList(),
    val warningsAr: List<String> = emptyList(),
)
