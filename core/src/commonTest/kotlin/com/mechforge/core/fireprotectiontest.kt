package com.mechforge.core

import com.mechforge.core.calcs.FirePumpHeadCalculator
import com.mechforge.core.calcs.FirePumpPowerCalculator
import com.mechforge.core.calcs.HoseNozzleFlowCalculator
import com.mechforge.core.calcs.SprinklerDischargeCalculator
import com.mechforge.core.calcs.WaterHammerCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SprinklerDischargeTest {

    @Test
    fun standardKFactorAtSevenPsi() {
        // K = 5.6 gpm/psi^0.5 at 7 psi -> 14.82 gpm = 56.1 L/min
        val out = T.run(
            SprinklerDischargeCalculator,
            T.iv("k", 5.6, "gpmpsi"), T.iv("p", 7.0, "psi"),
        )
        assertEquals(14.82, out.results.first { it.id == "qGpm" }.value, 0.05)
        assertEquals(56.09, out.results.first { it.id == "q" }.value, 0.3)
    }

    @Test
    fun metricKFactorCase() {
        // K = 80 L/min/bar^0.5 at 1 bar -> 80 L/min
        val out = T.run(
            SprinklerDischargeCalculator,
            T.iv("k", 80.0, "lpmbar"), T.iv("p", 1.0, "bar"),
        )
        assertEquals(80.0, out.results.first { it.id == "q" }.value, 0.1)
        assertEquals(4.8, out.results.first { it.id == "qM3h" }.value, 0.01)
    }

    @Test
    fun quadruplePressureDoublesFlow() {
        val base = T.run(
            SprinklerDischargeCalculator,
            T.iv("k", 5.6, "gpmpsi"), T.iv("p", 7.0, "psi"),
        ).results.first { it.id == "q" }.value
        val quad = T.run(
            SprinklerDischargeCalculator,
            T.iv("k", 5.6, "gpmpsi"), T.iv("p", 28.0, "psi"),
        ).results.first { it.id == "q" }.value
        assertEquals(2.0, quad / base, 1e-6)
    }

    @Test
    fun zeroPressureRejected() {
        try {
            T.run(SprinklerDischargeCalculator, T.iv("k", 5.6, "gpmpsi"), T.iv("p", 0.0, "psi"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "p" })
        }
    }
}

class HoseNozzleFlowTest {

    @Test
    fun textbookCase() {
        // 1.5 in nozzle at 100 psi -> 668.25 gpm
        val out = T.run(
            HoseNozzleFlowCalculator,
            T.iv("d", 1.5, "in"), T.iv("p", 100.0, "psi"),
        )
        assertEquals(668.25, out.results.first { it.id == "q" }.value, 1.0)
        assertEquals(2529.8, out.results.first { it.id == "qLmin" }.value, 5.0)
    }

    @Test
    fun oneInchNozzleCase() {
        // 1 in at 50 psi -> 29.7 * 1 * 7.0711 = 210.0 gpm
        val out = T.run(
            HoseNozzleFlowCalculator,
            T.iv("d", 1.0, "in"), T.iv("p", 50.0, "psi"),
        )
        assertEquals(210.0, out.results.first { it.id == "q" }.value, 0.5)
    }

    @Test
    fun metricInputsConverted() {
        // same duty expressed in mm and bar (38.1 mm = 1.5 in, 6.8948 bar = 100 psi)
        val out = T.run(
            HoseNozzleFlowCalculator,
            T.iv("d", 38.1, "mm"), T.iv("p", 6.894757, "bar"),
        )
        assertEquals(668.25, out.results.first { it.id == "q" }.value, 1.0)
    }

    @Test
    fun zeroDiameterRejected() {
        try {
            T.run(HoseNozzleFlowCalculator, T.iv("d", 0.0, "in"), T.iv("p", 100.0, "psi"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }
}

class FirePumpHeadTest {

    @Test
    fun boosterCase() {
        val out = T.run(
            FirePumpHeadCalculator,
            T.iv("preq", 8.0, "bar"), T.iv("pavail", 2.0, "bar"),
            T.iv("hstatic", 20.0, "m"), T.iv("hf", 12.0, "m"),
        )
        assertEquals(93.29, out.results.first { it.id == "h" }.value, 0.15)
        assertEquals(61.29, out.results.first { it.id == "phead" }.value, 0.1)
        assertEquals(6.0, out.results.first { it.id == "dp" }.value, 0.01)
    }

    @Test
    fun psiAndFeetCase() {
        // 100 psi required, 20 psi available, 50 ft static, 30 ft losses
        val out = T.run(
            FirePumpHeadCalculator,
            T.iv("preq", 100.0, "psi"), T.iv("pavail", 20.0, "psi"),
            T.iv("hstatic", 50.0, "ft"), T.iv("hf", 30.0, "ft"),
        )
        // pressure head = 80 psi / (998.2*9.80665) = 551580 Pa / 9789 = 56.35 m ; + 24.38 m = 80.73 m
        assertEquals(80.73, out.results.first { it.id == "h" }.value, 0.3)
    }

    @Test
    fun missingDensityWarns() {
        val out = T.run(
            FirePumpHeadCalculator,
            T.iv("preq", 8.0, "bar"), T.iv("hstatic", 20.0, "m"), T.iv("hf", 12.0, "m"),
        )
        assertTrue(out.warnings.any { it.contains("998.2") })
    }
}

class FirePumpPowerTest {

    @Test
    fun dutyPointCase() {
        val out = T.run(
            FirePumpPowerCalculator,
            T.iv("q", 2500.0, "lmin"), T.iv("h", 93.2935, "m"), T.iv("eta", 75.0, "pct"),
        )
        assertEquals(38.05, out.results.first { it.id == "hydraulic" }.value, 0.3)
        assertEquals(50.73, out.results.first { it.id == "shaft" }.value, 0.4)
        assertEquals(55.0, out.results.first { it.id == "motor" }.value, 1e-9)
    }

    @Test
    fun imperialFlowCase() {
        // 600 gpm, 300 ft, 70% : Q = 0.037854 m3/s, H = 91.44 m -> hydraulic = 33.94 kW
        val out = T.run(
            FirePumpPowerCalculator,
            T.iv("q", 600.0, "gpm"), T.iv("h", 300.0, "ft"), T.iv("eta", 70.0, "pct"),
        )
        assertEquals(33.94, out.results.first { it.id == "hydraulic" }.value, 0.3)
        assertEquals(48.48, out.results.first { it.id == "shaft" }.value, 0.4)
    }

    @Test
    fun belowLargestDriverWarns() {
        val out = T.run(
            FirePumpPowerCalculator,
            T.iv("q", 20000.0, "lmin"), T.iv("h", 400.0, "m"), T.iv("eta", 80.0, "pct"),
        )
        assertTrue(out.warnings.any { it.contains("200 kW") || it.contains("NFPA 20") })
    }
}

class WaterHammerTest {

    @Test
    fun joukowskyCase() {
        // 1000 kg/m3, c = 1200 m/s, dv = 2 m/s -> 24 bar / 244.7 m
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("c", 1200.0, "ms"), T.iv("dv", 2.0, "ms"),
        )
        assertEquals(24.0, out.results.first { it.id == "dp" }.value, 0.05)
        assertEquals(244.73, out.results.first { it.id == "head" }.value, 1.0)
    }

    @Test
    fun criticalClosureTimeCase() {
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("c", 1200.0, "ms"), T.iv("dv", 1.0, "ms"),
            T.iv("l", 1000.0, "m"),
        )
        assertEquals(1.6667, out.results.first { it.id == "tc" }.value, 0.01)
        assertEquals(12.0, out.results.first { it.id == "dp" }.value, 0.05)
    }

    @Test
    fun highVelocityChangeWarns() {
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("c", 1200.0, "ms"), T.iv("dv", 4.0, "ms"),
        )
        assertTrue(out.warnings.any { it.contains("very large surge") })
    }

    @Test
    fun zeroVelocityChangeRejected() {
        try {
            T.run(
                WaterHammerCalculator,
                T.iv("rho", 1000.0, "kgm3"), T.iv("c", 1200.0, "ms"), T.iv("dv", 0.0, "ms"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "dv" })
        }
    }
}
