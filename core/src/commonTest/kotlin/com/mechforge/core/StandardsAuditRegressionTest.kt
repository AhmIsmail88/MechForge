package com.mechforge.core

import com.mechforge.core.calcs.*
import com.mechforge.core.engine.*
import kotlin.test.*

/** Independent anchors and invalid-domain cases from the September 2026 technical audit. */
class StandardsAuditRegressionTest {
    private fun CalcOutput.v(id: String) = results.first { it.id == id }.value

    @Test fun siteDensityUsesKelvinExactlyOnceAcrossAllFiveConsumers() {
        // ISA sea-level pressure and dry-air gas constant: 101325/(287.05*293.15).
        val rho = 1.2041183163746156
        val site = arrayOf(T.iv("tair", 20.0, "c"), T.iv("alt", 0.0, "m"))
        val sensible = arrayOf(T.iv("q", 1.0, "m3s"), T.iv("tin", 30.0, "c"), T.iv("tout", 20.0, "c"))
        assertEquals(-rho * 1.005 * 10, T.run(SensibleHeatCalculator, *sensible, *site).v("qs"), 1e-9)
        val moisture = arrayOf(T.iv("win", 0.012, "dash"), T.iv("wout", 0.008, "dash"))
        assertEquals(-rho * 2501 * 0.004, T.run(LatentHeatCalculator, T.iv("q", 1.0, "m3s"), *moisture, *site).v("ql"), 1e-9)
        assertEquals(-rho * (1.005 * 10 + 2501 * 0.004), T.run(TotalCoolingLoadCalculator, *sensible, *moisture, *site).v("qt"), 1e-9)
        assertEquals(3600 * 10000 / (rho * 1005 * 10), T.run(HeatDissipationCalculator, T.iv("p", 10.0, "kw"), T.iv("dt", 10.0, "delk"), *site).v("q"), 1e-6)
        val duct = arrayOf(T.iv("v", 5.0, "ms"), T.iv("l", 10.0, "m"), T.iv("d", 500.0, "mm"))
        val explicit = T.run(DuctPressureLossCalculator, *duct, T.iv("rho", rho, "kgm3"))
        assertEquals(explicit.v("dp"), T.run(DuctPressureLossCalculator, *duct, *site).v("dp"), 1e-9)
    }

    @Test fun invalidOrIncompleteSiteDataAreRejected() {
        assertFailsWith<ValidationException> { airDensity(20.0, 12000.0) }
        assertFailsWith<ValidationException> { airDensity(-273.15, 0.0) }
        assertFailsWith<ValidationException> {
            T.run(HeatDissipationCalculator, T.iv("p", 10.0, "kw"), T.iv("dt", 10.0, "delk"), T.iv("tair", 20.0, "c"))
        }
    }

    @Test fun bearingReliability999UsesManufacturerIso281Factor() {
        val out = T.run(BearingL10Calculator, T.iv("c", 10.0, "kn"), T.iv("p", 1.0, "kn"), T.iv("rel", 7.0, "dash"))
        assertEquals(0.093, out.v("a1"), 1e-12)
        assertEquals(93_000_000.0, out.v("lnm"), 1e-6)
    }

    @Test fun class88M16Uses580MpaAndLargerDiameterUses600() {
        val shared = arrayOf(T.iv("class", 3.0, "dash"), T.iv("at", 157.0, "mm2"), T.iv("preloadpct", 65.0, "pct"))
        assertEquals(59.189, T.run(BoltTorqueCalculator, T.iv("d", 16.0, "mm"), *shared).v("fRec"), 1e-9)
        assertEquals(61.23, T.run(BoltTorqueCalculator, T.iv("d", 18.0, "mm"), *shared).v("fRec"), 1e-9)
        assertFailsWith<ValidationException> {
            T.run(BoltTorqueCalculator, T.iv("d", 16.0, "mm"), *shared, T.iv("f", 30.0, "kn"), T.iv("t", 100.0, "nm"))
        }
    }

    @Test fun wallEquationIncludesWAndMechanicalAllowance() {
        val base = arrayOf(T.iv("p", 10.0, "bar"), T.iv("d", 100.0, "mm"), T.iv("sigma", 100.0, "mpa"))
        val out = T.run(PipeWallThicknessCalculator, *base, T.iv("weld", 0.5, "dash"), T.iv("ma", 1.0, "mm"), T.iv("ca", 2.0, "mm"), T.iv("mill", 12.5, "pct"))
        // 1 MPa * 100 mm / [2*(100 MPa*0.5+1 MPa*0.4)] = 0.99206349 mm.
        assertEquals(0.9920634920634921, out.v("tp"), 1e-12)
        assertEquals(4.562358276643991, out.v("tNom"), 1e-12)
        assertFailsWith<ValidationException> {
            T.run(PipeWallThicknessCalculator, T.iv("p", 400.0, "bar"), T.iv("d", 100.0, "mm"), T.iv("sigma", 100.0, "mpa"))
        }
    }

    @Test fun fireMotorNeedsMaximumOverEntireCertifiedCurve() {
        val base = arrayOf(T.iv("q", 100.0, "m3h"), T.iv("h", 50.0, "m"), T.iv("eta", 75.0, "pct"))
        assertTrue(T.run(FirePumpPowerCalculator, *base, T.iv("bhp150", 30.0, "kw")).results.none { it.id == "motor" })
        val out = T.run(FirePumpPowerCalculator, *base, T.iv("bhpmax", 60.0, "kw"))
        assertEquals(75.0, out.v("motor"), 1e-12)
        assertFalse(out.results.first { it.id == "motor" }.isRecommended)
        assertFailsWith<ValidationException> { T.run(FirePumpPowerCalculator, *base, T.iv("bhpmax", 1.0, "kw")) }
    }

    @Test fun idealGasAgentEstimateCannotSelectCylinders() {
        for (calc in listOf(Fm200AgentQuantityCalculator, Co2AgentQuantityCalculator)) {
            val out = T.run(calc, T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"), T.iv("mcyl", 45.0, "kg"))
            assertTrue(out.results.none { it.id == "cylinders" || it.id == "installed" })
            assertTrue(out.results.first { it.id == "w" }.label.contains("Theoretical"))
            assertFalse(out.results.first { it.id == "w" }.isPrimary)
        }
    }

    @Test fun compressionUsesStricterOfThermalAndEnteredLimits() {
        val out = T.run(CompressionRatioCalculator, T.iv("p1", 1.0, "bar"), T.iv("p2", 16.0, "bar"), T.iv("crmax", 2.0, "dash"), T.iv("t1", 300.0, "k"), T.iv("t2max", 600.0, "k"))
        assertEquals(4.0, out.v("n"))
        assertFailsWith<ValidationException> { T.run(CompressionRatioCalculator, T.iv("p1", 1.0, "bar"), T.iv("p2", 16.0, "bar"), T.iv("crmax", 1.0, "dash")) }
    }

    @Test fun ntuRetainsVerySmallNonzeroEffectiveness() {
        val out = T.run(HxEffectivenessNtuCalculator, T.iv("ntu", 1e-12, "dash"), T.iv("cr", 0.5, "dash"))
        assertEquals(1e-12, out.v("epsFrac"), 1e-23)
        assertEquals(HxEffectivenessNtuCalculator.def.inputs.size, HxEffectivenessNtuCalculator.def.inputs.map { it.id }.distinct().size)
    }

    @Test fun geometryAndFractionalCountsAreNotSilentlyAccepted() {
        assertFailsWith<ValidationException> {
            T.run(DuctPressureLossCalculator, T.iv("v", 5.0, "ms"), T.iv("l", 10.0, "m"), T.iv("d", 500.0, "mm"), T.iv("w", 500.0, "mm"))
        }
        assertFailsWith<ValidationException> {
            T.run(HeatDissipationCalculator, T.iv("p", 10.0, "kw"), T.iv("dt", 10.0, "delk"), T.iv("nfans", 1.5, "dash"))
        }
        assertFailsWith<ValidationException> {
            T.run(LmtdCalculator, T.iv("thin", 50.0, "c"), T.iv("thout", 60.0, "c"), T.iv("tcin", 10.0, "c"), T.iv("tcout", 20.0, "c"))
        }
    }
}
