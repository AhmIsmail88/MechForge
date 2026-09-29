package com.mechforge.core.calcs

import kotlin.math.pow

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
internal fun airDensity(temperatureC: Double, altitudeM: Double): Double {
    val pressure = 101325.0 * (1.0 - 2.25577e-5 * altitudeM).pow(5.25588)
    return pressure / (287.05 * (273.15 + temperatureC))
}
