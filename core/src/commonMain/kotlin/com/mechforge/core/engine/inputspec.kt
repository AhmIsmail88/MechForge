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
    /**
     * Choices for a pick-one input (for example a hazard class). When present the input value
     * is the zero-based index of the chosen option (family DIMENSIONLESS), and the UI renders
     * a dropdown instead of a number field.
     */
    val options: List<InputOption>? = null,
    /**
     * Value the UI pre-fills, expressed in [defaultUnitId]. Only a convenience default:
     * the arithmetic still comes from whatever the user submits.
     */
    val defaultValue: Double? = null,
)

/** One choice of an option-based input. */
data class InputOption(val id: String, val label: String)

/** One user-provided input value, kept both in base SI and the unit it was entered in. */
data class InputValue(
    val inputId: String,
    val baseValue: Double,
    val displayUnitId: String,
)

data class InputError(val inputId: String, val message: String)

class ValidationException(val errors: List<InputError>) :
    Exception(errors.joinToString("; ") { it.message })
