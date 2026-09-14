package com.mechforge.core

import com.mechforge.core.calcs.AirflowConverterCalculator
import com.mechforge.core.calcs.PowerEfficiencyConverterCalculator
import com.mechforge.core.calcs.SensibleHeatCalculator
import com.mechforge.core.calcs.TotalCoolingLoadCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensibleHeatTest {

    @Test
    fun metricTextbookCase() {
        // V̇ = 0.5 m³/s, ΔT = 10 K, ρ = 1.2 → Q_s = 6.03 kW
        val out = T.run(
            SensibleHeatCalculator,
            T.iv("q", 1800.0, "m3h"), T.iv("tin", 20.0, "c"), T.iv("tout", 30.0, "c"),
        )
        assertEquals(6.030, out.results.first { it.id == "qs" }.value, 0.05)
    }

    @Test
    fun imperialWorkedExample() {
        // 1000 CFM, 70°F → 90°F: 1.08-form gives 21,600 BTU/h; metric path ≈ 21,580 BTU/h
        val out = T.run(
            SensibleHeatCalculator,
            T.iv("q", 1000.0, "cfm"), T.iv("tin", 70.0, "f"), T.iv("tout", 90.0, "f"),
        )
        val btuh = out.results.first { it.id == "qsBtuh" }.value
        assertEquals(21600.0, btuh, 220.0) // within ~1% of the 1.08-form
    }

    @Test
    fun heatingDirectionWarns() {
        val out = T.run(
            SensibleHeatCalculator,
            T.iv("q", 1000.0, "m3h"), T.iv("tin", 20.0, "c"), T.iv("tout", 30.0, "c"),
        )
        assertTrue(out.warnings.any { it.contains("heating") })
    }

    @Test
    fun equalTemperaturesWarns() {
        val out = T.run(
            SensibleHeatCalculator,
            T.iv("q", 1000.0, "m3h"), T.iv("tin", 20.0, "c"), T.iv("tout", 20.0, "c"),
        )
        assertEquals(0.0, out.results.first { it.id == "qs" }.value, 1e-9)
        assertTrue(out.warnings.any { it.contains("equal") })
    }

    @Test
    fun coolingGivesNegative() {
        val out = T.run(
            SensibleHeatCalculator,
            T.iv("q", 1000.0, "m3h"), T.iv("tin", 30.0, "c"), T.iv("tout", 20.0, "c"),
        )
        assertTrue(out.results.first { it.id == "qs" }.value < 0.0)
    }
}

class TotalCoolingLoadTest {

    @Test
    fun textbookCaseCooling() {
        // ṁ = 2.36 × 1.2 = 2.832 kg/s; ΔT = −11 K; Δw = −0.004
        val out = T.run(
            TotalCoolingLoadCalculator,
            T.iv("q", 8496.0, "m3h"),
            T.iv("tin", 24.0, "c"), T.iv("tout", 13.0, "c"),
            T.iv("win", 0.010, "dash"), T.iv("wout", 0.006, "dash"),
        )
        assertEquals(-31.31, out.results.first { it.id == "qs" }.value, 0.2)
        assertEquals(-28.33, out.results.first { it.id == "ql" }.value, 0.2)
        assertEquals(-59.64, out.results.first { it.id == "qt" }.value, 0.3)
    }

    @Test
    fun sensibleOnlyWithoutHumidity() {
        val out = T.run(
            TotalCoolingLoadCalculator,
            T.iv("q", 8496.0, "m3h"), T.iv("tin", 24.0, "c"), T.iv("tout", 13.0, "c"),
        )
        assertEquals(0.0, out.results.first { it.id == "ql" }.value, 1e-9)
        assertTrue(out.warnings.any { it.contains("Humidity") })
    }
}

class AirflowConverterTest {

    @Test
    fun cfmToAll() {
        val out = T.run(AirflowConverterCalculator, T.iv("q", 1000.0, "cfm"))
        assertEquals(1699.01, out.results.first { it.id == "m3h" }.value, 0.5)
        assertEquals(471.947, out.results.first { it.id == "ls" }.value, 0.2)
        assertEquals(28316.8, out.results.first { it.id == "lmin" }.value, 10.0)
    }

    @Test
    fun m3hToCfm() {
        val out = T.run(AirflowConverterCalculator, T.iv("q", 1.0, "m3h"))
        assertEquals(0.588578, out.results.first { it.id == "cfm" }.value, 0.0005)
    }
}

class PowerEfficiencyConverterTest {

    @Test
    fun trToKw() {
        val out = T.run(PowerEfficiencyConverterCalculator, T.iv("p", 100.0, "tr"))
        assertEquals(351.685, out.results.first { it.id == "kw" }.value, 0.05)
    }

    @Test
    fun kwToTr() {
        val out = T.run(PowerEfficiencyConverterCalculator, T.iv("p", 500.0, "kw"))
        assertEquals(142.185, out.results.first { it.id == "tr" }.value, 0.1)
    }

    @Test
    fun copEerKwPerTr() {
        // 10 TR (35.1685 kW) with 10 kW electrical → COP 3.517, EER ≈ 12.0, kW/TR = 1.0
        val out = T.run(
            PowerEfficiencyConverterCalculator,
            T.iv("p", 35.168528, "kw"), T.iv("pelec", 10.0, "kw"),
        )
        assertEquals(3.5168528, out.results.first { it.id == "cop" }.value, 0.001)
        assertEquals(11.9996, out.results.first { it.id == "eer" }.value, 0.02)
        assertEquals(1.0, out.results.first { it.id == "kwtr" }.value, 0.005)
    }
}
