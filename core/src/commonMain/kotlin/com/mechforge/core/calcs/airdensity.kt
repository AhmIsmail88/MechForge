package com.mechforge.core.calcs

import kotlin.math.pow
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException

/**
 * Dry-air density from temperature and site altitude.
 *
 *     P = 101325 * (1 - 2.25577e-5 * h)^5.25588   [Pa]   (ISA pressure profile)
 *     rho = P / (287.05 * (273.15 + T))            [kg/m3]
 *
 * Hot or high sites change the answer materially: 1.2 kg/m3 is the 20 C sea-level
 * shorthand, while 45 C at 500 m gives about 1.06 kg/m3 - 12 % less air for the same
 * volume flow, so ventilation airflow must scale with it. This helper is shared by the
 * airflow and load calculators so they cannot disagree with each other.
 */
internal fun atmosphericPressure(altitudeM: Double): Double {
    if (altitudeM !in -5000.0..11000.0) {
        throw ValidationException(listOf(InputError("alt", "The standard-atmosphere approximation supports altitudes from -5000 to 11000 m.")))
    }
    return 101325.0 * (1.0 - 2.25577e-5 * altitudeM).pow(5.25588)
}

internal fun airDensity(temperatureC: Double, altitudeM: Double): Double {
    if (!temperatureC.isFinite() || temperatureC <= -273.15) {
        throw ValidationException(listOf(InputError("tair", "Air temperature must be above absolute zero.")))
    }
    val pressure = atmosphericPressure(altitudeM)
    return pressure / (287.05 * (273.15 + temperatureC))
}

internal data class AirDensityResult(val value: Double, val warnings: List<String>, val warningsAr: List<String>)

/** InputValue stores absolute temperature in kelvin, regardless of its display unit. */
internal fun resolveAirDensity(inputs: Map<String, InputValue>): AirDensityResult {
    inputs["rho"]?.let { return AirDensityResult(it.baseValue, emptyList(), emptyList()) }
    val temperature = inputs["tair"]?.baseValue
    val altitude = inputs["alt"]?.baseValue
    if ((temperature == null) != (altitude == null)) {
        throw ValidationException(listOf(InputError(if (temperature == null) "tair" else "alt", "Enter both air temperature and altitude, or enter density directly.")))
    }
    if (temperature == null || altitude == null) return AirDensityResult(
        1.2,
        listOf("Air density assumed as 1.2 kg/m3; enter actual density or both temperature and altitude."),
        listOf("افتُرضت كثافة الهواء 1.2 كغ/م³؛ أدخل الكثافة الفعلية أو درجة الحرارة والارتفاع معًا."),
    )
    val density = airDensity(temperature - 273.15, altitude)
    return AirDensityResult(
        density,
        listOf("Air density from ${temperature - 273.15} C at $altitude m = $density kg/m3 (dry-air approximation)."),
        listOf("كثافة الهواء من درجة حرارة ${temperature - 273.15} °C وارتفاع $altitude م = $density كغ/م³ (تقريب الهواء الجاف)."),
    )
}
