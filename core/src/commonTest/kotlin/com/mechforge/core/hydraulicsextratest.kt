package com.mechforge.core

import com.mechforge.core.calcs.FrictionFactorCalculator
import com.mechforge.core.calcs.HazenWilliamsCalculator
import com.mechforge.core.calcs.ManningCalculator
import com.mechforge.core.calcs.MinorLossCalculator
import com.mechforge.core.calcs.NpshAvailableCalculator
import com.mechforge.core.calcs.OrificeFlowCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MinorLossTest {

    @Test
    fun textbookCase() {
        val out = T.run(MinorLossCalculator, T.iv("k", 0.5, "dash"), T.iv("v", 2.0, "ms"))
        assertEquals(0.102, out.results.first { it.id == "hm" }.value, 0.001)
    }

    @Test
    fun imperialVelocityCase() {
        // 10 ft/s with K = 1.0 → 1 × (3.048)²/(2×9.80665) = 0.4737 m
        val out = T.run(MinorLossCalculator, T.iv("k", 1.0, "dash"), T.iv("v", 10.0, "fts"))
        assertEquals(0.4737, out.results.first { it.id == "hm" }.value, 0.005)
    }

    @Test
    fun zeroVelocityRejected() {
        try {
            T.run(MinorLossCalculator, T.iv("k", 1.0, "dash"), T.iv("v", 0.0, "ms"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "v" })
        }
    }
}

class FrictionFactorCalculatorTest {

    @Test
    fun turbulentPublishedValue() {
        // Re = 1e5, ε/D = 2e-4 → f ≈ 0.0190
        val out = T.run(
            FrictionFactorCalculator,
            T.iv("re", 100000.0, "dash"), T.iv("eps", 0.02, "mm"), T.iv("d", 100.0, "mm"),
        )
        assertEquals(0.0190, out.results.first { it.id == "f" }.value, 0.0002)
    }

    @Test
    fun laminarExact() {
        val out = T.run(
            FrictionFactorCalculator,
            T.iv("re", 1000.0, "dash"), T.iv("d", 100.0, "mm"),
        )
        assertEquals(0.064, out.results.first { it.id == "f" }.value, 1e-9)
    }

    @Test
    fun excessiveRoughnessRejected() {
        try {
            T.run(
                FrictionFactorCalculator,
                T.iv("re", 100000.0, "dash"), T.iv("eps", 50.0, "mm"), T.iv("d", 100.0, "mm"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "eps" })
        }
    }
}

class OrificeFlowTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            OrificeFlowCalculator,
            T.iv("cd", 0.62, "dash"), T.iv("d", 50.0, "mm"), T.iv("h", 2.0, "m"),
        )
        assertEquals(27.45, out.results.first { it.id == "q" }.value, 0.3)
    }

    @Test
    fun imperialHeadCase() {
        // 10 ft (3.048 m) head, 2 in orifice, Cd 0.62 → √(2gH) = 7.7318 m/s, Q = 34.98 m³/h
        val out = T.run(
            OrificeFlowCalculator,
            T.iv("cd", 0.62, "dash"), T.iv("d", 2.0, "in"), T.iv("h", 10.0, "ft"),
        )
        assertEquals(34.98, out.results.first { it.id == "q" }.value, 0.4)
    }

    @Test
    fun defaultCoefficientWarns() {
        val out = T.run(OrificeFlowCalculator, T.iv("d", 50.0, "mm"), T.iv("h", 2.0, "m"))
        assertTrue(out.warnings.any { it.contains("0.62") })
    }
}

class ManningTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            ManningCalculator,
            T.iv("n", 0.013, "dash"), T.iv("r", 0.075, "m"), T.iv("s", 0.001, "dash"),
            T.iv("a", 0.070686, "m2"),
        )
        assertEquals(0.4326, out.results.first { it.id == "v" }.value, 0.005)
        assertEquals(110.1, out.results.first { it.id == "q" }.value, 1.5)
    }

    @Test
    fun steeperSlopeIncreasesFlow() {
        val gentle = T.run(
            ManningCalculator,
            T.iv("n", 0.013, "dash"), T.iv("r", 0.075, "m"), T.iv("s", 0.001, "dash"), T.iv("a", 0.070686, "m2"),
        ).results.first { it.id == "q" }.value
        val steep = T.run(
            ManningCalculator,
            T.iv("n", 0.013, "dash"), T.iv("r", 0.075, "m"), T.iv("s", 0.004, "dash"), T.iv("a", 0.070686, "m2"),
        ).results.first { it.id == "q" }.value
        assertEquals(2.0, steep / gentle, 0.05) // S^0.5 → 2× for 4× slope
    }

    @Test
    fun zeroSlopeRejected() {
        try {
            T.run(
                ManningCalculator,
                T.iv("n", 0.013, "dash"), T.iv("r", 0.075, "m"), T.iv("s", 0.0, "dash"), T.iv("a", 0.1, "m2"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "s" })
        }
    }
}

class HazenWilliamsTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            HazenWilliamsCalculator,
            T.iv("c", 130.0, "dash"), T.iv("d", 300.0, "mm"), T.iv("s", 0.001, "dash"),
        )
        assertEquals(131.1, out.results.first { it.id == "q" }.value, 1.5)
    }

    @Test
    fun imperialDiameterCase() {
        // 12 in (304.8 mm) diameter, C = 130, S = 0.001 → ≈136.9 m³/h (D^2.63 scaling)
        val out = T.run(
            HazenWilliamsCalculator,
            T.iv("c", 130.0, "dash"), T.iv("d", 12.0, "in"), T.iv("s", 0.001, "dash"),
        )
        assertEquals(136.9, out.results.first { it.id == "q" }.value, 1.5)
    }

    @Test
    fun zeroDiameterRejected() {
        try {
            T.run(
                HazenWilliamsCalculator,
                T.iv("c", 130.0, "dash"), T.iv("d", 0.0, "mm"), T.iv("s", 0.001, "dash"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }
}

class NpshAvailableTest {

    @Test
    fun textbookCaseWithDefaults() {
        val out = T.run(
            NpshAvailableCalculator,
            T.iv("hs", 3.0, "m"), T.iv("hf", 1.5, "m"),
        )
        assertEquals(11.61, out.results.first { it.id == "npsha" }.value, 0.03)
        assertTrue(out.warnings.any { it.contains("2.339 kPa") })
    }

    @Test
    fun lowMarginWarns() {
        // Pump lifted 6 m from an open tank: 10.11 − 6.0 − 2.5 = 1.61 m → low margin
        val out = T.run(
            NpshAvailableCalculator,
            T.iv("hs", -6.0, "m"), T.iv("hf", 2.5, "m"),
        )
        assertEquals(1.61, out.results.first { it.id == "npsha" }.value, 0.03)
        assertTrue(out.warnings.any { it.contains("low margin") })
    }

    @Test
    fun hotWaterReducesNpsh() {
        val cold = T.run(
            NpshAvailableCalculator,
            T.iv("pv", 2.339, "kpa"), T.iv("hs", 3.0, "m"), T.iv("hf", 1.5, "m"),
        ).results.first { it.id == "npsha" }.value
        val hot = T.run(
            NpshAvailableCalculator,
            T.iv("pv", 47.4, "kpa"), T.iv("hs", 3.0, "m"), T.iv("hf", 1.5, "m"),
        ).results.first { it.id == "npsha" }.value
        assertTrue(hot < cold, "NPSHa should fall with higher vapour pressure")
    }
}
