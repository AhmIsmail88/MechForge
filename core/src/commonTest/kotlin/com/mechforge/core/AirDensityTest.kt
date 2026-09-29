package com.mechforge.core

import com.mechforge.core.calcs.SensibleHeatCalculator
import com.mechforge.core.calcs.airDensity
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Air density from temperature and altitude, and the route that uses it.
 *
 * The two anchors are the standard-atmosphere values: 15 C at sea level is the classic
 * 1.225 kg/m3 and 20 C at sea level is 1.2041 kg/m3. Hot or high sites move the density far
 * enough to matter: at 45 C and 500 m the air is more than 12 % thinner than the 1.2 kg/m3
 * shorthand that the airflow calculators use, so the same volume flow carries less air.
 */
class AirDensityTest {

    @Test
    fun standardAtmosphereValuesAreReproduced() {
        assertEquals(1.225, airDensity(15.0, 0.0), 0.002)
        assertEquals(1.2041, airDensity(20.0, 0.0), 0.002)
    }

    @Test
    fun hotAndHighSitesAreMateriallyThinner() {
        val hot = airDensity(45.0, 500.0)
        assertTrue(hot < 1.1, "45 C at 500 m gave $hot kg/m3")
        assertTrue(abs(hot - 1.2) / 1.2 > 0.05, "the shorthand differs by more than 5 %")
        assertTrue(airDensity(45.0, 0.0) < airDensity(20.0, 0.0), "hotter air is thinner")
        assertTrue(airDensity(20.0, 1500.0) < airDensity(20.0, 0.0), "higher sites are thinner")
    }

    @Test
    fun theCalculatorUsesTheSiteConditionsWhenTheyAreGiven() {
        // 1 m3/s of air at 20 C sea level is the reference case
        val base = SensibleHeatCalculator.run(
            mapOf(
                "q" to com.mechforge.core.engine.InputValue("q", 1.0, "m3s"),
                "tin" to com.mechforge.core.engine.InputValue("tin", 20.0, "c"),
                "tout" to com.mechforge.core.engine.InputValue("tout", 30.0, "c"),
            ),
        )
        val hot = SensibleHeatCalculator.run(
            mapOf(
                "q" to com.mechforge.core.engine.InputValue("q", 1.0, "m3s"),
                "tin" to com.mechforge.core.engine.InputValue("tin", 20.0, "c"),
                "tout" to com.mechforge.core.engine.InputValue("tout", 30.0, "c"),
                "tair" to com.mechforge.core.engine.InputValue("tair", 45.0, "c"),
                "alt" to com.mechforge.core.engine.InputValue("alt", 500.0, "m"),
            ),
        )
        val baseQ = base.results.first { it.id == "q" }.value
        val hotQ = hot.results.first { it.id == "q" }.value
        assertTrue(hotQ < baseQ, "thinner air carries less heat for the same volume flow")
        assertTrue(abs(hotQ - baseQ) / baseQ > 0.1, "the difference should be material")
        assertTrue(hot.warnings.any { it.contains("Air density from") }, hot.warnings.toString())
    }
}
