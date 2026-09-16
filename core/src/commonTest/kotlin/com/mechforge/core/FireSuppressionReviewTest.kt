package com.mechforge.core

import com.mechforge.core.calcs.Co2AgentQuantityCalculator
import com.mechforge.core.calcs.Fm200AgentQuantityCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the FM-200 (NFPA 2001) and CO2 (NFPA 12) review corrections.
 *
 * The expected numbers are the worked examples in the two review documents, verified
 * independently here:
 *
 *   FM-200, V_net = 47.04 m3, Class C (7.0 %), T = 21 C
 *     S = 8.31446261815324*294.15/(101325*0.17003) = 0.141958 m3/kg
 *     W_basic = (47.04/0.141958)*(7/93) = 24.9414 kg
 *     30 kg listed cylinder -> 1 cylinder, margin 30 - 24.9414 = 5.0586 kg (20.28 %)
 *   CO2, V_net = 47.04 m3, deep-seated dry electrical (50 %), NFPA 12 table f = 1.60
 *     W_basic = 47.04 * 1.60 = 75.264 kg
 *     45 kg cylinder -> 2 cylinders (NOT 3), installed 90 kg, margin 14.736 kg
 */
class FireSuppressionReviewTest {

    @Test
    fun fm200MatchesTheReviewExampleAndShowsTheCylinderMargin() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 2.0, "dash"), // Class C
            T.iv("t", 21.0, "c"),
            T.iv("mcyl", 30.0, "kg"),
        )
        assertEquals(7.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(0.141958, out.results.first { it.id == "sUsed" }.value, 1e-5)
        assertEquals(24.9414, out.results.first { it.id == "wbasic" }.value, 0.01)
        assertEquals(0.0, out.results.first { it.id == "wadd" }.value, 1e-9)
        assertEquals(24.9414, out.results.first { it.id == "w" }.value, 0.01)
        assertEquals(1.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
        assertEquals(30.0, out.results.first { it.id == "installed" }.value, 1e-9)
        assertEquals(5.0586, out.results.first { it.id == "margin" }.value, 0.01)
        assertEquals(20.28, out.results.first { it.id == "marginPct" }.value, 0.05)
    }

    @Test
    fun fm200AdditionalQuantityIsAddedSeparatelyAndNeverPercentaged() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 2.0, "dash"),
            T.iv("t", 21.0, "c"),
            T.iv("addkg", 2.5, "kg"),
        )
        assertEquals(24.9414, out.results.first { it.id == "wbasic" }.value, 0.01)
        assertEquals(2.5, out.results.first { it.id == "wadd" }.value, 1e-9)
        assertEquals(27.4414, out.results.first { it.id == "w" }.value, 0.01)
        // the basic quantity must not have been inflated by any percentage
        assertTrue(out.steps.any { it.contains("Additional quantity = 2.50 kg") })
    }

    @Test
    fun fm200VolumeCrossCheckCatchesADecimalSlip() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 4704.0, "m3"),
            T.iv("vgross", 50.0, "m3"),
            T.iv("hazard", 0.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertTrue(
            out.warnings.any { it.contains("larger than the gross room volume") },
            "the 100x decimal slip must be flagged: ${out.warnings}",
        )
        // and the equivalent room size is printed so the engineer can sanity-check the input
        assertTrue(out.steps.any { it.contains("equivalent room size") })
    }

    @Test
    fun fm200GrossMinusExcludedMismatchIsFlagged() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("vgross", 60.0, "m3"),
            T.iv("vexcl", 5.0, "m3"),
            T.iv("hazard", 0.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertTrue(out.warnings.any { it.contains("does not match the entered net volume") })
    }

    @Test
    fun fm200ClassCWarnsAboutEnergizedElectricalHazards() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 2.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertTrue(out.warnings.any { it.contains("480 V") }, "Class C needs the hazard-analysis warning")
    }

    @Test
    fun fm200NeverAddsALeakageOrPipingAllowance() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 2.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertEquals(0.0, out.results.first { it.id == "wadd" }.value, 1e-12)
        assertTrue(out.warnings.any { it.contains("No leakage, piping or reserve allowance") })
        assertTrue(out.steps.any { it.contains("Additional quantity = 0 kg") })
    }

    @Test
    fun co2MatchesTheNfpa12ReviewExampleWithTheTableFloodingFactor() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 4.0, "dash"), // deep-seated dry electrical
            T.iv("t", 21.0, "c"),
            T.iv("ftable", 1.60, "kgm3"),
            T.iv("mcyl", 45.0, "kg"),
        )
        assertEquals(50.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(1.60, out.results.first { it.id == "f" }.value, 1e-9)
        assertEquals(1.823329, out.results.first { it.id == "fIdeal" }.value, 1e-5)
        assertEquals(75.264, out.results.first { it.id == "wbasic" }.value, 0.01)
        assertEquals(75.264, out.results.first { it.id == "w" }.value, 0.01)
        // the review document is explicit: 2 cylinders, never 3
        assertEquals(2.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
        assertEquals(90.0, out.results.first { it.id == "installed" }.value, 1e-9)
        assertEquals(14.736, out.results.first { it.id == "margin" }.value, 0.01)
    }

    @Test
    fun co2WithoutATableFactorSaysTheFactorIsAnEstimate() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 4.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertEquals(1.823329, out.results.first { it.id == "f" }.value, 1e-5)
        assertTrue(out.warnings.any { it.contains("ideal-gas") && it.contains("NFPA 12") })
        assertTrue(out.steps.any { it.contains("ideal-gas estimate") })
    }

    @Test
    fun co2ReportsZeroAdditionalQuantityExplicitly() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 0.0, "dash"),
            T.iv("t", 21.0, "c"),
        )
        assertEquals(0.0, out.results.first { it.id == "wadd" }.value, 1e-12)
        assertTrue(out.steps.any { it.contains("Additional CO2 quantity = 0 kg") })
        assertTrue(out.warnings.any { it.contains("No additional allowance is added automatically") })
    }

    @Test
    fun co2UnclosableOpeningsQuantityIsAddedSeparately() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 47.04, "m3"),
            T.iv("hazard", 4.0, "dash"),
            T.iv("t", 21.0, "c"),
            T.iv("ftable", 1.60, "kgm3"),
            T.iv("addkg", 10.0, "kg"),
            T.iv("mcyl", 45.0, "kg"),
        )
        assertEquals(75.264, out.results.first { it.id == "wbasic" }.value, 0.01)
        assertEquals(10.0, out.results.first { it.id == "wadd" }.value, 1e-9)
        assertEquals(85.264, out.results.first { it.id == "w" }.value, 0.01)
        assertEquals(2.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
    }
}
