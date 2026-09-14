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
)
