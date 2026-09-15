package com.mechforge.core.calcs

/**
 * IEC 60034-1 preferred (standard) output ratings in kW.
 *
 * Driver sizing practice: pick the first standard rating at or above the calculated
 * shaft power. Kept in one place so every driver-sizing calculator uses the same list.
 */
internal val IEC_MOTOR_RATINGS_KW = doubleArrayOf(
    0.55, 0.75, 1.1, 1.5, 2.2, 3.0, 4.0, 5.5, 7.5, 11.0, 15.0,
    18.5, 22.0, 30.0, 37.0, 45.0, 55.0, 75.0, 90.0, 110.0, 132.0, 160.0, 200.0,
)

/** First standard IEC rating at or above [powerKw], or null when it is beyond the list. */
internal fun nextIecMotorRating(powerKw: Double): Double? =
    IEC_MOTOR_RATINGS_KW.firstOrNull { it >= powerKw }
