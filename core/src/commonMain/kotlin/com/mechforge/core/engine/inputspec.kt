package com.mechforge.core.engine

import com.mechforge.core.units.UnitFamily

/**
 * Declaration of one calculator input. Ranges are expressed in the family BASE unit
 * (SI), independent of the display unit the user picks.
 */
data class InputSpec(
    val id: String,
    val label: String,
    val symbol: String,
    val family: UnitFamily,
    val required: Boolean = true,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val exclusiveMin: Boolean = false,
    val exclusiveMax: Boolean = false,
    val allowedUnitIds: List<String>? = null,
    val defaultUnitId: String? = null,
    /**
     * Optional key of a reference-library dataset that can fill this input
     * (for example "roughness" or "density"). The app maps the key to a dataset.
     */
    val libraryKey: String? = null,
)

/** One user-provided input value, kept both in base SI and the unit it was entered in. */
data class InputValue(
    val inputId: String,
    val baseValue: Double,
    val displayUnitId: String,
)

data class InputError(val inputId: String, val message: String)

class ValidationException(val errors: List<InputError>) :
    Exception(errors.joinToString("; ") { it.message })
