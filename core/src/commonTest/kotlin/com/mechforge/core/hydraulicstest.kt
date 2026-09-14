package com.mechforge.core

import com.mechforge.core.calcs.DarcyWeisbachCalculator
import com.mechforge.core.calcs.PipeSizingCalculator
import com.mechforge.core.calcs.PipeVelocityCalculator
import com.mechforge.core.calcs.PumpPowerCalculator
import com.mechforge.core.calcs.ReynoldsNumberCalculator
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.math.FrictionFactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PumpPowerTest {

    @Test
    fun readmeWorkedExample() {
        val out = T.run(
            PumpPowerCalculator,
            T.iv("q", 100.0, "m3h"),
            T.iv("h", 50.0, "m"),
            T.iv("eta", 80.0, "pct"),
            T.iv("rho", 1000.0, "kgm3"),
        )
        val hydraulic = out.results.first { it.id == "hydraulic" }
        val shaft = out.results.first { it.id == "shaft" }
        val motor = out.results.first { it.id == "motor" }
        assertEquals(13.62, hydraulic.value, 0.15)
        assertEquals(17.03, shaft.value, 0.15)
        assertEquals(18.5, motor.value, 1e-9)
        assertTrue(motor.isRecommended)
    }

    @Test
    fun imperialDutyCase() {
        val out = T.run(
            PumpPowerCalculator,
            T.iv("q", 500.0, "gpm"),
            T.iv("h", 100.0, "ft"),
            T.iv("eta", 70.0, "pct"),
        )
        assertEquals(9.43, out.results.first { it.id == "hydraulic" }.value, 0.15)
        assertEquals(13.47, out.results.first { it.id == "shaft" }.value, 0.2)
        assertEquals(15.0, out.results.first { it.id == "motor" }.value, 1e-9)
        assertTrue(out.warnings.any { it.contains("1000 kg/m") })
    }

    @Test
    fun efficiencyAbove100Rejected() {
        try {
            T.run(PumpPowerCalculator, T.iv("q", 100.0, "m3h"), T.iv("h", 50.0, "m"), T.iv("eta", 120.0, "pct"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "eta" })
        }
    }

    @Test
    fun zeroAndNegativeInputsRejected() {
        try {
            T.run(PumpPowerCalculator, T.iv("q", 0.0, "m3h"), T.iv("h", 50.0, "m"), T.iv("eta", 80.0, "pct"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "q" })
        }
    }

    @Test
    fun highEfficiencyWarns() {
        val out = T.run(
            PumpPowerCalculator,
            T.iv("q", 100.0, "m3h"), T.iv("h", 50.0, "m"), T.iv("eta", 90.0, "pct"),
        )
        assertTrue(out.warnings.any { it.contains("optimistic") })
    }

    @Test
    fun exceedsLargestMotorWarns() {
        val out = T.run(
            PumpPowerCalculator,
            T.iv("q", 2000.0, "m3h"), T.iv("h", 150.0, "m"), T.iv("eta", 75.0, "pct"),
        )
        assertTrue(out.warnings.any { it.contains("90 kW") })
    }
}

class PipeVelocityTest {

    @Test
    fun textbookCase() {
        val out = T.run(PipeVelocityCalculator, T.iv("q", 100.0, "m3h"), T.iv("d", 100.0, "mm"))
        assertEquals(3.5368, out.results.first().value, 0.01)
    }

    @Test
    fun imperialCase() {
        // 500 GPM through a 4-inch internal diameter
        val out = T.run(PipeVelocityCalculator, T.iv("q", 500.0, "gpm"), T.iv("d", 4.0, "in"))
        assertEquals(3.891, out.results.first().value, 0.03)
    }

    @Test
    fun lowVelocityWarns() {
        val out = T.run(PipeVelocityCalculator, T.iv("q", 1.0, "m3h"), T.iv("d", 100.0, "mm"))
        assertTrue(out.warnings.any { it.contains("0.3 m/s") })
    }
}

class ReynoldsTest {

    @Test
    fun turbulentCase() {
        val out = T.run(ReynoldsNumberCalculator, T.iv("v", 1.0, "ms"), T.iv("d", 50.0, "mm"), T.iv("nu", 1.0, "cst"))
        assertEquals(50000.0, out.results.first().value, 1.0)
        assertTrue(out.warnings.isEmpty())
    }

    @Test
    fun laminarCase() {
        val out = T.run(ReynoldsNumberCalculator, T.iv("v", 0.02, "ms"), T.iv("d", 50.0, "mm"), T.iv("nu", 1.0, "cst"))
        assertEquals(1000.0, out.results.first().value, 1.0)
    }

    @Test
    fun transitionalWarns() {
        val out = T.run(ReynoldsNumberCalculator, T.iv("v", 0.046, "ms"), T.iv("d", 50.0, "mm"), T.iv("nu", 1.0, "cst"))
        assertEquals(2300.0, out.results.first().value, 5.0)
        assertTrue(out.warnings.any { it.contains("transitional") })
    }
}

class DarcyWeisbachTest {

    @Test
    fun fixedFrictionFactorTextbookCase() {
        val out = T.run(
            DarcyWeisbachCalculator,
            T.iv("v", 2.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("l", 100.0, "m"), T.iv("f", 0.02, "dash"),
        )
        assertEquals(4.078, out.results.first { it.id == "hf" }.value, 0.05)
    }

    @Test
    fun colebrookSmoothPipePublishedValue() {
        val out = T.run(
            DarcyWeisbachCalculator,
            T.iv("v", 2.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("l", 100.0, "m"), T.iv("re", 100000.0, "dash"),
        )
        val f = out.results.first { it.id == "f" }.value
        assertEquals(0.0180, f, 0.0180 * 0.015) // within 1.5% of the Moody-chart/Blasius-region value
        assertTrue(out.warnings.any { it.contains("smooth") })
    }

    @Test
    fun laminarUses64OverRe() {
        val out = T.run(
            DarcyWeisbachCalculator,
            T.iv("v", 0.02, "ms"), T.iv("d", 50.0, "mm"), T.iv("l", 10.0, "m"), T.iv("re", 1000.0, "dash"),
        )
        assertEquals(0.064, out.results.first { it.id == "f" }.value, 1e-9)
    }

    @Test
    fun roughPipeColebrook() {
        val out = T.run(
            DarcyWeisbachCalculator,
            T.iv("v", 2.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("l", 100.0, "m"),
            T.iv("re", 200000.0, "dash"), T.iv("eps", 0.01, "mm"),
        )
        val f = out.results.first { it.id == "f" }.value
        assertEquals(0.0164, f, 0.0164 * 0.02) // Swamee-Jain seed region, 2% tolerance
    }

    @Test
    fun bothFrictionSourcesRejected() {
        try {
            T.run(
                DarcyWeisbachCalculator,
                T.iv("v", 2.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("l", 100.0, "m"),
                T.iv("re", 100000.0, "dash"), T.iv("f", 0.02, "dash"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
    }

    @Test
    fun noFrictionSourceRejected() {
        try {
            T.run(
                DarcyWeisbachCalculator,
                T.iv("v", 2.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("l", 100.0, "m"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "re" })
        }
    }
}

class FrictionFactorMathTest {

    @Test
    fun colebrookRoughTurbulentPublishedCase() {
        // Published benchmark: Re=1e6, ε/D=1e-4 → f ≈ 0.0135 (Moody / Swamee-Jain agreement)
        val f = FrictionFactor.darcy(1e6, 1e-4)
        assertEquals(0.0135, f, 0.0135 * 0.015)
    }

    @Test
    fun laminarExact() {
        assertEquals(64.0 / 1500.0, FrictionFactor.darcy(1500.0, 0.05), 1e-12)
    }
}

class PipeSizingTest {

    @Test
    fun metricCase() {
        val out = T.run(PipeSizingCalculator, T.iv("q", 100.0, "m3h"), T.iv("v", 2.0, "ms"))
        assertEquals(132.97, out.results.first().value, 0.5)
    }

    @Test
    fun imperialCase() {
        // 500 GPM at 8 ft/s
        val out = T.run(PipeSizingCalculator, T.iv("q", 500.0, "gpm"), T.iv("v", 8.0, "fts"))
        assertEquals(128.4, out.results.first().value, 1.0)
    }

    @Test
    fun highVelocityWarns() {
        val out = T.run(PipeSizingCalculator, T.iv("q", 100.0, "m3h"), T.iv("v", 4.0, "ms"))
        assertTrue(out.warnings.any { it.contains("3 m/s") })
    }
}
