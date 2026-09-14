package com.mechforge.core

import com.mechforge.core.calcs.AirChangesCalculator
import com.mechforge.core.calcs.DuctPressureLossCalculator
import com.mechforge.core.calcs.DuctSizingCalculator
import com.mechforge.core.calcs.DuctVelocityCalculator
import com.mechforge.core.calcs.FanPowerCalculator
import com.mechforge.core.calcs.LatentHeatCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class LatentHeatTest {

    @Test
    fun humidificationCase() {
        // 2 m³/s, ρ=1.2, w 0.004 → 0.009 → 2.4 × 2501 × 0.005 = 30.01 kW
        val out = T.run(
            LatentHeatCalculator,
            T.iv("q", 2.0, "m3s"), T.iv("win", 0.004, "dash"), T.iv("wout", 0.009, "dash"),
        )
        assertEquals(30.01, out.results.first { it.id == "ql" }.value, 0.1)
        assertTrue(out.warnings.any { it.contains("ADDED") })
    }

    @Test
    fun dehumidificationIsNegative() {
        val out = T.run(
            LatentHeatCalculator,
            T.iv("q", 2.0, "m3s"), T.iv("win", 0.009, "dash"), T.iv("wout", 0.004, "dash"),
        )
        assertTrue(out.results.first { it.id == "ql" }.value < 0.0)
        assertTrue(out.warnings.any { it.contains("REMOVED") })
    }

    @Test
    fun imperialAirflowCase() {
        // 1000 CFM = 0.471947 m³/s; ṁ = 0.566337 kg/s; Δw=0.005 → 7.08 kW
        val out = T.run(
            LatentHeatCalculator,
            T.iv("q", 1000.0, "cfm"), T.iv("win", 0.004, "dash"), T.iv("wout", 0.009, "dash"),
        )
        assertEquals(7.082, out.results.first { it.id == "ql" }.value, 0.05)
    }
}

class DuctVelocityTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            DuctVelocityCalculator,
            T.iv("q", 1.0, "m3s"), T.iv("w", 400.0, "mm"), T.iv("h", 300.0, "mm"),
        )
        assertEquals(8.333, out.results.first { it.id == "v" }.value, 0.01)
        assertTrue(out.warnings.any { it.contains("8 m/s") })
    }

    @Test
    fun imperialReportedInFpm() {
        val out = T.run(
            DuctVelocityCalculator,
            T.iv("q", 1.0, "m3s"), T.iv("w", 400.0, "mm"), T.iv("h", 300.0, "mm"),
        )
        assertEquals(8.333 / 0.00508, out.results.first { it.id == "vFpm" }.value, 2.0)
    }

    @Test
    fun zeroWidthRejected() {
        try {
            T.run(
                DuctVelocityCalculator,
                T.iv("q", 1.0, "m3s"), T.iv("w", 0.0, "mm"), T.iv("h", 300.0, "mm"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "w" })
        }
    }
}

class DuctSizingTest {

    @Test
    fun textbookCase() {
        val out = T.run(DuctSizingCalculator, T.iv("q", 1.0, "m3s"), T.iv("v", 6.0, "ms"))
        assertEquals(0.1667, out.results.first { it.id == "a" }.value, 0.001)
        assertEquals(460.7, out.results.first { it.id == "deq" }.value, 1.0)
    }

    @Test
    fun imperialAirflowWithAspectRatio() {
        val out = T.run(
            DuctSizingCalculator,
            T.iv("q", 2000.0, "cfm"), T.iv("v", 1000.0, "fpm"), T.iv("r", 2.0, "dash"),
        )
        val deq = out.results.first { it.id == "deq" }.value
        // 2000 CFM = 0.943895 m³/s at 5.08 m/s → A = 0.18581 m² → Deq = 486.4 mm
        assertEquals(486.4, deq, 3.0)
    }

    @Test
    fun zeroVelocityRejected() {
        try {
            T.run(DuctSizingCalculator, T.iv("q", 1.0, "m3s"), T.iv("v", 0.0, "ms"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "v" })
        }
    }
}

class DuctPressureLossTest {

    @Test
    fun roundDuctPublishedCase() {
        // D=0.4 m, v=6 m/s, ν=1.5e-5, ε=9e-5, L=10 m → Δp ≈ 9.6 Pa
        val out = T.run(
            DuctPressureLossCalculator,
            T.iv("v", 6.0, "ms"), T.iv("l", 10.0, "m"), T.iv("d", 400.0, "mm"),
        )
        assertEquals(9.61, out.results.first { it.id == "dp" }.value, 0.4)
    }

    @Test
    fun rectangularHydraulicDiameter() {
        // W=0.5, H=0.3 → Dh = 2×0.5×0.3/0.8 = 0.375 m
        val out = T.run(
            DuctPressureLossCalculator,
            T.iv("v", 6.0, "ms"), T.iv("l", 10.0, "m"),
            T.iv("w", 500.0, "mm"), T.iv("h", 300.0, "mm"),
        )
        assertEquals(375.0, out.results.first { it.id == "dh" }.value, 0.5)
    }

    @Test
    fun missingGeometryRejected() {
        try {
            T.run(DuctPressureLossCalculator, T.iv("v", 6.0, "ms"), T.iv("l", 10.0, "m"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }
}

class FanPowerTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            FanPowerCalculator,
            T.iv("q", 1.0, "m3s"), T.iv("dp", 500.0, "pa"), T.iv("etaf", 65.0, "pct"),
        )
        assertEquals(0.7692, out.results.first { it.id == "pshaft" }.value, 0.005)
        assertEquals(0.7692, out.results.first { it.id == "pmotor" }.value, 0.005)
    }

    @Test
    fun imperialPressureCase() {
        // 2000 CFM at 2 in.wg (498.2 Pa), η=0.7 → P = 0.9439×498.2/0.7 = 671.6 W
        val out = T.run(
            FanPowerCalculator,
            T.iv("q", 2000.0, "cfm"), T.iv("dp", 498.18, "pa"), T.iv("etaf", 70.0, "pct"),
        )
        assertEquals(0.6716, out.results.first { it.id == "pshaft" }.value, 0.01)
    }

    @Test
    fun efficiencyAbove100Rejected() {
        try {
            T.run(
                FanPowerCalculator,
                T.iv("q", 1.0, "m3s"), T.iv("dp", 500.0, "pa"), T.iv("etaf", 120.0, "pct"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "etaf" })
        }
    }
}

class AirChangesTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            AirChangesCalculator,
            T.iv("q", 1.0, "m3s"), T.iv("vroom", 240.0, "m3"),
        )
        assertEquals(15.0, out.results.first { it.id == "ach" }.value, 0.01)
    }

    @Test
    fun imperialAirflowCase() {
        // 2000 CFM = 3398.02 m³/h; V=300 m³ → 11.33 ACH
        val out = T.run(
            AirChangesCalculator,
            T.iv("q", 2000.0, "cfm"), T.iv("vroom", 300.0, "m3"),
        )
        assertEquals(11.33, out.results.first { it.id == "ach" }.value, 0.05)
    }

    @Test
    fun zeroVolumeRejected() {
        try {
            T.run(AirChangesCalculator, T.iv("q", 1.0, "m3s"), T.iv("vroom", 0.0, "m3"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "vroom" })
        }
    }
}
