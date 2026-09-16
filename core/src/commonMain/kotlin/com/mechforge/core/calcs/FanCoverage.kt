package com.mechforge.core.calcs

import com.mechforge.core.util.Fmt
import kotlin.math.ceil

/**
 * Relating a computed airflow requirement to the fans that will serve it.
 *
 * The ventilation calculators report the airflow a space needs; an engineer then asks the
 * practical question - how many fans of the size I am going to select does that take, and how
 * much margin does the selection carry? These functions answer that, and they are pure so the
 * calculators and the tests share one implementation.
 *
 * All airflow values are in m3/s (the FLOW base unit).
 */
object FanCoverage {

    /** Fans of [capacityPerFan] needed to reach [required], or null when no capacity was given. */
    fun fansNeeded(required: Double, capacityPerFan: Double?): Int? {
        if (capacityPerFan == null || capacityPerFan <= 0.0) return null
        if (required <= 0.0) return 0
        return ceil(required / capacityPerFan - 1e-9).toInt()
    }

    /** Airflow selected by [count] fans of [capacityPerFan], or null when either is missing. */
    fun provided(count: Double?, capacityPerFan: Double?): Double? {
        if (count == null || capacityPerFan == null) return null
        if (count <= 0.0 || capacityPerFan <= 0.0) return null
        return count * capacityPerFan
    }

    /** Selected capacity relative to the requirement, in percent, or null when it cannot form. */
    fun marginPercent(providedValue: Double?, required: Double): Double? {
        if (providedValue == null || required <= 0.0) return null
        return (providedValue / required - 1.0) * 100.0
    }

    /**
     * What the stated selection says about itself: short capacity, too few fans to reach the
     * requirement, or a very large oversize. Empty when the selection covers the requirement
     * within reason, and empty when no fan data was entered at all.
     */
    fun warnings(
        required: Double,
        providedValue: Double?,
        fansNeeded: Int?,
        count: Double?,
    ): List<String> = buildList {
        val installed = providedValue ?: return@buildList
        if (required > 0.0 && installed < required) {
            val short = (required - installed) / required * 100.0
            add(
                "Selected fan capacity is short of the calculated requirement by " +
                    Fmt.n(short, 1) + " % - add capacity or revisit the target."
            )
        }
        if (fansNeeded != null && count != null && count > 0.0 && count < fansNeeded) {
            add(
                "The stated number of fans cannot deliver the calculated airflow: " +
                    "$fansNeeded fan(s) of that capacity are needed."
            )
        }
        val margin = marginPercent(installed, required)
        if (margin != null && margin > 100.0) {
            add(
                "Selected capacity is more than double the requirement (+" + Fmt.n(margin, 0) +
                    " %) - check the fan selection for efficiency, noise and control range."
            )
        }
    }
}
