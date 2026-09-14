package com.mechforge.core

import com.mechforge.core.calcs.CarnotEfficiencyCalculator
import com.mechforge.core.calcs.CompressorPowerCalculator
import com.mechforge.core.calcs.IsentropicRelationCalculator
import com.mechforge.core.calcs.LmtdCalculator
import com.mechforge.core.calcs.ThermalEfficiencyCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CarnotEfficiencyTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            CarnotEfficiencyCalculator,
            T.iv("th", 600.0, "k"), T.iv("tc", 300.0, "k"),
        )
        assertEquals(50.0, out.results.first { it.id == "eta" }.value, 0.01)
    }

    @Test
    fun celsiusInputsCase() {
        // 327 °C = 600.15 K, 26.85 °C = 300 K → 1 − 300/600.15 = 0.5001 → 50.01 %
        val out = T.run(
            CarnotEfficiencyCalculator,
            T.iv("th", 327.0, "c"), T.iv("tc", 26.85, "c"),
        )
        assertEquals(50.01, out.results.first { it.id == "eta" }.value, 0.05)
    }

    @Test
    fun coldHotterThanHotRejected() {
        try {
            T.run(CarnotEfficiencyCalculator, T.iv("th", 300.0, "k"), T.iv("tc", 600.0, "k"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "tc" })
        }
    }
}

class ThermalEfficiencyTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            ThermalEfficiencyCalculator,
            T.iv("w", 800.0, "kj"), T.iv("q", 2000.0, "kj"),
        )
        assertEquals(40.0, out.results.first { it.id == "eta" }.value, 0.01)
        assertEquals(1200.0, out.results.first { it.id == "rejected" }.value, 0.5)
    }

    @Test
    fun kwhInputCase() {
        // 1 kWh = 3600 kJ work from 7200 kJ heat → 50 %
        val out = T.run(
            ThermalEfficiencyCalculator,
            T.iv("w", 1.0, "kwh"), T.iv("q", 7200.0, "kj"),
        )
        assertEquals(50.0, out.results.first { it.id == "eta" }.value, 0.01)
    }

    @Test
    fun workExceedingHeatRejected() {
        try {
            T.run(ThermalEfficiencyCalculator, T.iv("w", 3000.0, "kj"), T.iv("q", 2000.0, "kj"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "w" })
        }
    }
}

class IsentropicRelationTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            IsentropicRelationCalculator,
            T.iv("p1", 100.0, "kpa"), T.iv("p2", 800.0, "kpa"), T.iv("t1", 300.0, "k"),
        )
        assertEquals(543.4, out.results.first { it.id == "t2" }.value, 1.0)
        assertEquals(1.8114, out.results.first { it.id == "tratio" }.value, 0.005)
    }

    @Test
    fun barPressureAndCelsiusCase() {
        // P ratio 6, k = 1.4, T1 = 20 °C = 293.15 K → T2 = 293.15×6^0.2857 = 489.3 K
        val out = T.run(
            IsentropicRelationCalculator,
            T.iv("p1", 1.0, "bar"), T.iv("p2", 6.0, "bar"), T.iv("t1", 20.0, "c"),
        )
        assertEquals(489.3, out.results.first { it.id == "t2" }.value, 1.0)
    }

    @Test
    fun defaultRatioWarns() {
        val out = T.run(
            IsentropicRelationCalculator,
            T.iv("p1", 100.0, "kpa"), T.iv("p2", 800.0, "kpa"), T.iv("t1", 300.0, "k"),
        )
        assertTrue(out.warnings.any { it.contains("k = 1.4") })
    }
}

class CompressorPowerTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            CompressorPowerCalculator,
            T.iv("m", 3600.0, "kgh"), T.iv("t1", 300.0, "k"),
            T.iv("p1", 100.0, "kpa"), T.iv("p2", 800.0, "kpa"),
        )
        assertEquals(543.4, out.results.first { it.id == "t2s" }.value, 1.0)
        assertEquals(604.3, out.results.first { it.id == "t2a" }.value, 1.5)
        assertEquals(305.8, out.results.first { it.id == "pshaft" }.value, 3.0)
    }

    @Test
    fun celsiusAndCustomEfficiencyCase() {
        val out = T.run(
            CompressorPowerCalculator,
            T.iv("m", 3600.0, "kgh"), T.iv("t1", 27.0, "c"),
            T.iv("p1", 100.0, "kpa"), T.iv("p2", 800.0, "kpa"),
            T.iv("eta", 75.0, "pct"),
        )
        // T1 = 300.15 K, T2s = 543.7 K, T2a = 300.15 + 243.5/0.75 = 624.8 K
        assertEquals(624.8, out.results.first { it.id == "t2a" }.value, 2.0)
    }

    @Test
    fun nonIncreasingPressureRejected() {
        try {
            T.run(
                CompressorPowerCalculator,
                T.iv("m", 3600.0, "kgh"), T.iv("t1", 300.0, "k"),
                T.iv("p1", 800.0, "kpa"), T.iv("p2", 800.0, "kpa"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "p2" })
        }
    }
}

class LmtdTest {

    @Test
    fun counterCurrentTextbookCase() {
        val out = T.run(
            LmtdCalculator,
            T.iv("thin", 90.0, "c"), T.iv("thout", 60.0, "c"),
            T.iv("tcin", 20.0, "c"), T.iv("tcout", 45.0, "c"),
        )
        assertEquals(42.45, out.results.first { it.id == "lmtd" }.value, 0.1)
    }

    @Test
    fun equalTerminalDifferencesCase() {
        // ΔT1 = ΔT2 = 40 K → LMTD = 40 K exactly
        val out = T.run(
            LmtdCalculator,
            T.iv("thin", 90.0, "c"), T.iv("thout", 70.0, "c"),
            T.iv("tcin", 30.0, "c"), T.iv("tcout", 50.0, "c"),
        )
        assertEquals(40.0, out.results.first { it.id == "lmtd" }.value, 0.01)
    }

    @Test
    fun dutyWithUAndArea() {
        // U = 500 W/(m²·K), A = 10 m², LMTD = 42.45 K → Q = 212.25 kW
        val out = T.run(
            LmtdCalculator,
            T.iv("thin", 90.0, "c"), T.iv("thout", 60.0, "c"),
            T.iv("tcin", 20.0, "c"), T.iv("tcout", 45.0, "c"),
            T.iv("u", 500.0, "wm2k"), T.iv("a", 10.0, "m2"),
        )
        assertEquals(212.25, out.results.first { it.id == "q" }.value, 0.5)
    }

    @Test
    fun crossingTemperaturesRejected() {
        try {
            T.run(
                LmtdCalculator,
                T.iv("thin", 60.0, "c"), T.iv("thout", 50.0, "c"),
                T.iv("tcin", 80.0, "c"), T.iv("tcout", 90.0, "c"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
    }
}
