package com.mechforge.core

import com.mechforge.core.calcs.AirChangesCalculator
import com.mechforge.core.calcs.FanCoverage
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shared fan-cover logic, and the proof that the ACH calculator sizes fans with it too.
 */
class FanCoverageTest {

    @Test
    fun fansNeededRoundsUpAndAnExactFitIsEnough() {
        assertEquals(1, FanCoverage.fansNeeded(0.5, 1.0))
        assertEquals(2, FanCoverage.fansNeeded(2.0, 1.0), "an exact fit must not ask for a third fan")
        assertEquals(3, FanCoverage.fansNeeded(2.0000001, 1.0))
        assertEquals(3, FanCoverage.fansNeeded(2.9, 1.0))
        assertNull(FanCoverage.fansNeeded(1.0, null))
        assertNull(FanCoverage.fansNeeded(1.0, 0.0))
        assertEquals(0, FanCoverage.fansNeeded(0.0, 1.0))
    }

    @Test
    fun selectedCapacityNeedsBothNumbers() {
        assertEquals(3.0, FanCoverage.provided(3.0, 1.0)!!, 1e-12)
        assertNull(FanCoverage.provided(null, 1.0))
        assertNull(FanCoverage.provided(3.0, null))
        assertNull(FanCoverage.provided(0.0, 1.0))
        assertNull(FanCoverage.provided(3.0, 0.0))
    }

    @Test
    fun marginIsRelativeToTheRequirement() {
        assertEquals(0.0, FanCoverage.marginPercent(2.0, 2.0)!!, 1e-12)
        assertEquals(50.0, FanCoverage.marginPercent(3.0, 2.0)!!, 1e-12)
        assertEquals(-25.0, FanCoverage.marginPercent(1.5, 2.0)!!, 1e-12)
        assertNull(FanCoverage.marginPercent(null, 2.0))
        assertNull(FanCoverage.marginPercent(2.0, 0.0))
    }

    @Test
    fun theWarningsSpeakOnlyAboutWhatWasEntered() {
        assertTrue(
            FanCoverage.warnings(1.0, null, null, null).isEmpty(),
            "no fan data means no fan comment",
        )
        assertTrue(
            FanCoverage.warnings(1.0, 1.05, 1, 1.0).isEmpty(),
            "a covered selection raises nothing",
        )

        val short = FanCoverage.warnings(1.0, 0.8, 2, 1.0)
        assertTrue(short.any { it.contains("short of the calculated requirement") }, "$short")
        assertTrue(short.any { it.contains("2 fan(s)") }, "one fan cannot cover a two-fan duty: $short")

        val huge = FanCoverage.warnings(1.0, 2.5, 1, 1.0)
        assertEquals(1, huge.size, "only the oversize comment applies here: $huge")
        assertTrue(huge.first().contains("more than double"), "$huge")
    }

    @Test
    fun theAchCalculatorSizesFansAgainstItsComputedAirflow() {
        // 15 ACH on 240 m3 = 3600 m3/h required; fans of 1200 m3/h each, two selected
        val out = AirChangesCalculator.run(
            mapOf(
                "vroom" to InputValue("vroom", 240.0, "m3"),
                "ach" to InputValue("ach", 15.0, "perh"),
                "fancap" to InputValue("fancap", Units.byId("m3h").toBase(1200.0), "m3h"),
                "nfans" to InputValue("nfans", 2.0, "dash"),
            )
        )
        fun value(id: String) = out.results.first { it.id == id }.value
        assertEquals(3600.0, value("q"), 1e-6)
        assertEquals(3.0, value("fansNeeded"), 1e-9)
        assertEquals(2400.0, value("fanTotal"), 1e-6)
        assertTrue(
            out.warnings.any { it.contains("short of the calculated requirement") },
            "two fans of 1200 cannot cover 3600: ${out.warnings}",
        )

        // and without the fan fields the calculator behaves exactly as before
        val plain = AirChangesCalculator.run(
            mapOf(
                "vroom" to InputValue("vroom", 240.0, "m3"),
                "ach" to InputValue("ach", 15.0, "perh"),
            )
        )
        assertTrue(plain.results.none { it.id == "fansNeeded" })
        assertTrue(plain.warnings.none { it.contains("short of the calculated requirement") })
    }
}
