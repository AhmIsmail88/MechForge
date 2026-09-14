package com.mechforge.core

import com.mechforge.core.calcs.ChlorineDoseCalculator
import com.mechforge.core.calcs.DetentionTimeCalculator
import com.mechforge.core.calcs.HydraulicLoadingCalculator
import com.mechforge.core.calcs.PeakFlowCalculator
import com.mechforge.core.calcs.TankVolumeCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class TankVolumeTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            TankVolumeCalculator,
            T.iv("q", 100.0, "m3h"), T.iv("t", 2.0, "h"),
        )
        assertEquals(200.0, out.results.first { it.id == "v" }.value, 0.5)
    }

    @Test
    fun litresOutputCase() {
        val out = T.run(
            TankVolumeCalculator,
            T.iv("q", 10.0, "ls"), T.iv("t", 300.0, "s"),
        )
        // 0.01 m³/s × 300 s = 3 m³
        assertEquals(3.0, out.results.first { it.id == "v" }.value, 0.01)
        assertEquals(3000.0, out.results.first { it.id == "vl" }.value, 1.0)
    }

    @Test
    fun zeroDetentionRejected() {
        try {
            T.run(TankVolumeCalculator, T.iv("q", 100.0, "m3h"), T.iv("t", 0.0, "h"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "t" })
        }
    }
}

class DetentionTimeTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            DetentionTimeCalculator,
            T.iv("v", 500.0, "m3"), T.iv("q", 250.0, "m3h"),
        )
        assertEquals(2.0, out.results.first { it.id == "t" }.value, 0.01)
        assertEquals(120.0, out.results.first { it.id == "tm" }.value, 0.5)
    }

    @Test
    fun litresPerSecondCase() {
        // 100 m³ at 50 L/s (0.05 m³/s) → 2000 s = 0.5556 h
        val out = T.run(
            DetentionTimeCalculator,
            T.iv("v", 100.0, "m3"), T.iv("q", 50.0, "ls"),
        )
        assertEquals(0.5556, out.results.first { it.id == "t" }.value, 0.005)
    }

    @Test
    fun zeroFlowRejected() {
        try {
            T.run(DetentionTimeCalculator, T.iv("v", 500.0, "m3"), T.iv("q", 0.0, "m3h"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "q" })
        }
    }
}

class ChlorineDoseTest {

    @Test
    fun textbookCase() {
        // 5000 m³/d at 2 mg/L → 10 kg/d
        val out = T.run(
            ChlorineDoseCalculator,
            T.iv("q", 5000.0, "m3d"), T.iv("dose", 2.0, "mgl"),
        )
        assertEquals(10.0, out.results.first { it.id == "mr" }.value, 0.02)
        assertEquals(0.4167, out.results.first { it.id == "mh" }.value, 0.005)
    }

    @Test
    fun ppmExpressedInMgPerLitre() {
        // 1000 m³/d at 5 mg/L → 5 kg/d
        val out = T.run(
            ChlorineDoseCalculator,
            T.iv("q", 1000.0, "m3d"), T.iv("dose", 5.0, "mgl"),
        )
        assertEquals(5.0, out.results.first { it.id == "mr" }.value, 0.02)
    }

    @Test
    fun productStrengthWarningPresent() {
        val out = T.run(
            ChlorineDoseCalculator,
            T.iv("q", 1000.0, "m3d"), T.iv("dose", 2.0, "mgl"),
        )
        assertTrue(out.warnings.any { it.contains("PURE chemical") })
    }

    @Test
    fun zeroDoseRejected() {
        try {
            T.run(ChlorineDoseCalculator, T.iv("q", 1000.0, "m3d"), T.iv("dose", 0.0, "mgl"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "dose" })
        }
    }
}

class PeakFlowTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            PeakFlowCalculator,
            T.iv("q", 200.0, "m3h"), T.iv("pf", 2.5, "dash"),
        )
        assertEquals(500.0, out.results.first { it.id == "qp" }.value, 0.5)
        assertEquals(12000.0, out.results.first { it.id == "qpd" }.value, 10.0)
    }

    @Test
    fun litresPerSecondPeak() {
        // 100 L/s × 1.8 → 180 L/s = 648 m³/h
        val out = T.run(
            PeakFlowCalculator,
            T.iv("q", 100.0, "ls"), T.iv("pf", 1.8, "dash"),
        )
        assertEquals(648.0, out.results.first { it.id == "qp" }.value, 1.0)
    }

    @Test
    fun peakingFactorBelowOneRejected() {
        try {
            T.run(PeakFlowCalculator, T.iv("q", 200.0, "m3h"), T.iv("pf", 0.5, "dash"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "pf" })
        }
    }
}

class HydraulicLoadingTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            HydraulicLoadingCalculator,
            T.iv("q", 1000.0, "m3d"), T.iv("a", 250.0, "m2"),
        )
        assertEquals(4.0, out.results.first { it.id == "hlr" }.value, 0.01)
        assertEquals(0.1667, out.results.first { it.id == "hlrh" }.value, 0.001)
    }

    @Test
    fun perHourFlowCase() {
        // 500 m³/h over 500 m² → 24 m/d
        val out = T.run(
            HydraulicLoadingCalculator,
            T.iv("q", 500.0, "m3h"), T.iv("a", 500.0, "m2"),
        )
        assertEquals(24.0, out.results.first { it.id == "hlr" }.value, 0.05)
    }

    @Test
    fun zeroAreaRejected() {
        try {
            T.run(HydraulicLoadingCalculator, T.iv("q", 1000.0, "m3d"), T.iv("a", 0.0, "m2"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "a" })
        }
    }
}
