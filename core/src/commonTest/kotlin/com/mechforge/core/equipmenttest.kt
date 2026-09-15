package com.mechforge.core

import com.mechforge.core.calcs.CompressionRatioCalculator
import com.mechforge.core.calcs.FanLawsCalculator
import com.mechforge.core.calcs.HeatExchangerDutyCalculator
import com.mechforge.core.calcs.HxEffectivenessNtuCalculator
import com.mechforge.core.calcs.PumpAffinityLawsCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HeatExchangerDutyTest {

    @Test
    fun waterSideCase() {
        // 2 kg/s, cp = 4186, 60 -> 80 C : Q = 167.44 kW
        val out = T.run(
            HeatExchangerDutyCalculator,
            T.iv("m", 2.0, "kgs"), T.iv("cp", 4.186, "kjkgk"),
            T.iv("tin", 60.0, "c"), T.iv("tout", 80.0, "c"),
        )
        assertEquals(167.44, out.results.first { it.id == "q" }.value, 0.3)
        assertEquals(571347.0, out.results.first { it.id == "qBtuh" }.value, 2000.0)
    }

    @Test
    fun areaSideCrossCheck() {
        val out = T.run(
            HeatExchangerDutyCalculator,
            T.iv("m", 1.0, "kgs"), T.iv("cp", 4.186, "kjkgk"),
            T.iv("tin", 30.0, "c"), T.iv("tout", 50.0, "c"),
            T.iv("u", 500.0, "wm2k"), T.iv("a", 10.0, "m2"),
            T.iv("thin", 90.0, "c"), T.iv("thout", 60.0, "c"),
            T.iv("tcin", 20.0, "c"), T.iv("tcout", 45.0, "c"), T.iv("arr", 1.0, "dash"),
        )
        assertEquals(83.72, out.results.first { it.id == "q" }.value, 0.3)
        assertEquals(42.45, out.results.first { it.id == "lmtd" }.value, 0.1)
        assertEquals(212.25, out.results.first { it.id == "qArea" }.value, 0.5)
    }

    @Test
    fun missingAreaSideWarns() {
        val out = T.run(
            HeatExchangerDutyCalculator,
            T.iv("m", 2.0, "kgs"), T.iv("tin", 60.0, "c"), T.iv("tout", 80.0, "c"),
        )
        assertTrue(out.warnings.any { it.contains("Area side") })
        assertTrue(out.warnings.any { it.contains("4186") })
    }
}

class HxEffectivenessTest {

    @Test
    fun phaseChangeCase() {
        // NTU = 2, Cr = 0 -> 86.47 %
        val out = T.run(
            HxEffectivenessNtuCalculator,
            T.iv("ntu", 2.0, "dash"), T.iv("cr", 0.0, "dash"),
        )
        assertEquals(86.47, out.results.first { it.id == "eps" }.value, 0.05)
    }

    @Test
    fun counterFlowEqualCapacities() {
        // NTU = 2, Cr = 1, counter -> NTU/(1+NTU) = 66.67 %
        val out = T.run(
            HxEffectivenessNtuCalculator,
            T.iv("ntu", 2.0, "dash"), T.iv("cr", 1.0, "dash"), T.iv("arr", 1.0, "dash"),
        )
        assertEquals(66.67, out.results.first { it.id == "eps" }.value, 0.05)
    }

    @Test
    fun parallelFlowEqualCapacities() {
        // NTU = 2, Cr = 1, parallel -> (1-exp(-4))/2 = 49.08 %
        val out = T.run(
            HxEffectivenessNtuCalculator,
            T.iv("ntu", 2.0, "dash"), T.iv("cr", 1.0, "dash"), T.iv("arr", 0.0, "dash"),
        )
        assertEquals(49.08, out.results.first { it.id == "eps" }.value, 0.05)
    }

    @Test
    fun capacityRatioAboveOneRejected() {
        try {
            T.run(
                HxEffectivenessNtuCalculator,
                T.iv("ntu", 2.0, "dash"), T.iv("cr", 1.5, "dash"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "cr" })
        }
    }
}

class PumpAffinityLawsTest {

    @Test
    fun speedIncreaseCase() {
        val out = T.run(
            PumpAffinityLawsCalculator,
            T.iv("q1", 100.0, "m3h"), T.iv("h1", 50.0, "m"), T.iv("p1", 10.0, "kw"),
            T.iv("n1", 1500.0, "rpm"), T.iv("n2", 1800.0, "rpm"),
        )
        assertEquals(120.0, out.results.first { it.id == "q2" }.value, 0.01)
        assertEquals(72.0, out.results.first { it.id == "h2" }.value, 0.01)
        assertEquals(17.28, out.results.first { it.id == "p2" }.value, 0.02)
    }

    @Test
    fun trimCase() {
        val out = T.run(
            PumpAffinityLawsCalculator,
            T.iv("q1", 100.0, "m3h"), T.iv("h1", 50.0, "m"), T.iv("p1", 10.0, "kw"),
            T.iv("n1", 1500.0, "rpm"), T.iv("n2", 1500.0, "rpm"),
            T.iv("d1", 200.0, "mm"), T.iv("d2", 180.0, "mm"),
        )
        assertEquals(90.0, out.results.first { it.id == "q2t" }.value, 0.01)
        assertEquals(40.5, out.results.first { it.id == "h2t" }.value, 0.01)
        assertEquals(7.29, out.results.first { it.id == "p2t" }.value, 0.02)
    }

    @Test
    fun largeTrimWarns() {
        val out = T.run(
            PumpAffinityLawsCalculator,
            T.iv("q1", 100.0, "m3h"), T.iv("h1", 50.0, "m"), T.iv("p1", 10.0, "kw"),
            T.iv("n1", 1500.0, "rpm"), T.iv("n2", 1500.0, "rpm"),
            T.iv("d1", 200.0, "mm"), T.iv("d2", 150.0, "mm"),
        )
        assertTrue(out.warnings.any { it.contains("Trim beyond") })
    }

    @Test
    fun zeroSpeedRejected() {
        try {
            T.run(
                PumpAffinityLawsCalculator,
                T.iv("q1", 100.0, "m3h"), T.iv("h1", 50.0, "m"), T.iv("p1", 10.0, "kw"),
                T.iv("n1", 0.0, "rpm"), T.iv("n2", 1800.0, "rpm"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "n1" })
        }
    }
}

class FanLawsTest {

    @Test
    fun speedIncreaseSameDensity() {
        val out = T.run(
            FanLawsCalculator,
            T.iv("q1", 10000.0, "m3h"), T.iv("dp1", 500.0, "pa"), T.iv("p1", 5.0, "kw"),
            T.iv("n1", 1000.0, "rpm"), T.iv("n2", 1200.0, "rpm"),
        )
        assertEquals(12000.0, out.results.first { it.id == "q2" }.value, 1.0)
        assertEquals(720.0, out.results.first { it.id == "dp2" }.value, 1.0)
        assertEquals(8.64, out.results.first { it.id == "p2" }.value, 0.02)
    }

    @Test
    fun densityCorrectionCase() {
        // rho2/rho1 = 1.08/1.2 = 0.9 -> dp2 = 720*0.9 = 648 Pa, p2 = 8.64*0.9 = 7.776 kW
        val out = T.run(
            FanLawsCalculator,
            T.iv("q1", 10000.0, "m3h"), T.iv("dp1", 500.0, "pa"), T.iv("p1", 5.0, "kw"),
            T.iv("n1", 1000.0, "rpm"), T.iv("n2", 1200.0, "rpm"),
            T.iv("rho1", 1.2, "kgm3"), T.iv("rho2", 1.08, "kgm3"),
        )
        assertEquals(648.0, out.results.first { it.id == "dp2" }.value, 1.0)
        assertEquals(7.776, out.results.first { it.id == "p2" }.value, 0.02)
        assertEquals(0.9, out.results.first { it.id == "rd" }.value, 1e-9)
    }

    @Test
    fun missingDensityWarns() {
        val out = T.run(
            FanLawsCalculator,
            T.iv("q1", 10000.0, "m3h"), T.iv("dp1", 500.0, "pa"), T.iv("p1", 5.0, "kw"),
            T.iv("n1", 1000.0, "rpm"), T.iv("n2", 1200.0, "rpm"),
        )
        assertTrue(out.warnings.any { it.contains("1.2 kg/m3") })
    }
}

class CompressionRatioTest {

    @Test
    fun twoStageCase() {
        val out = T.run(
            CompressionRatioCalculator,
            T.iv("p1", 1.0, "bar"), T.iv("p2", 16.0, "bar"), T.iv("crmax", 4.0, "dash"),
        )
        assertEquals(16.0, out.results.first { it.id == "cr" }.value, 1e-9)
        assertEquals(2.0, out.results.first { it.id == "n" }.value, 1e-9)
        assertEquals(4.0, out.results.first { it.id == "crStage" }.value, 1e-9)
        assertEquals(4.0, out.results.first { it.id == "pint" }.value, 1e-9)
    }

    @Test
    fun singleStageWhenWithinLimit() {
        val out = T.run(
            CompressionRatioCalculator,
            T.iv("p1", 1.0, "bar"), T.iv("p2", 3.0, "bar"), T.iv("crmax", 4.0, "dash"),
        )
        assertEquals(1.0, out.results.first { it.id == "n" }.value, 1e-9)
        assertEquals(3.0, out.results.first { it.id == "crStage" }.value, 1e-9)
    }

    @Test
    fun psiInputCase() {
        // 14.5 psi -> 232 psi is about 1 -> 16 bar
        val out = T.run(
            CompressionRatioCalculator,
            T.iv("p1", 14.5038, "psi"), T.iv("p2", 232.06, "psi"),
        )
        assertEquals(16.0, out.results.first { it.id == "cr" }.value, 0.05)
    }

    @Test
    fun lowerDischargeRejected() {
        try {
            T.run(
                CompressionRatioCalculator,
                T.iv("p1", 10.0, "bar"), T.iv("p2", 5.0, "bar"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "p2" })
        }
    }
}
