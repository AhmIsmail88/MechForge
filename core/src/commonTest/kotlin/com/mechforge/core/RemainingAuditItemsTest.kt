package com.mechforge.core

import com.mechforge.core.calcs.*
import com.mechforge.core.engine.*
import kotlin.test.*

class RemainingAuditItemsTest {
    private fun CalcOutput.v(id: String) = results.first { it.id == id }.value
    private val valve = arrayOf(T.iv("kv", 10.0, "dash"), T.iv("p1", 10.0, "bar"),
        T.iv("pv", 1.0, "bar"), T.iv("pc", 100.0, "bar"), T.iv("fl", 0.8, "dash"))

    @Test fun chokedCapacityStopsIncreasingWithPressureDrop() {
        // FF=.96-.28*sqrt(1/100)=.932; dPchoked=.8²*(10-.932)=5.80352 bar.
        val a = T.run(ValveKvCalculator, *valve, T.iv("dp", 7.0, "bar"))
        val b = T.run(ValveKvCalculator, *valve, T.iv("dp", 9.5, "bar"))
        assertEquals(0.932, a.v("ff"), 1e-12)
        assertEquals(5.80352, a.v("dpChoked"), 1e-10)
        assertEquals(a.v("q"), b.v("q"), 1e-12)
        assertTrue(a.warnings.any { it.contains("Flow is choked") })
        assertTrue(b.warnings.any { it.contains("flashing service") })
    }

    @Test fun attachedFittingsUseBothManufacturerFactors() {
        val out = T.run(ValveKvCalculator, *valve, T.iv("dp", 2.0, "bar"), T.iv("fp", 0.9, "dash"), T.iv("flp", 0.72, "dash"))
        assertEquals(12.727922061357855, out.v("q"), 1e-10)
        assertEquals(5.80352, out.v("dpChoked"), 1e-10)
    }

    @Test fun incompleteAndImpossibleLiquidInputsAreRejected() {
        assertFailsWith<ValidationException> { T.run(ValveKvCalculator, T.iv("kv", 10.0, "dash"), T.iv("dp", 2.0, "bar"), T.iv("p1", 10.0, "bar")) }
        assertFailsWith<ValidationException> { T.run(ValveKvCalculator, *valve, T.iv("dp", 10.0, "bar")) }
        assertFailsWith<ValidationException> { T.run(ValveKvCalculator, *valve, T.iv("dp", 2.0, "bar"), T.iv("fp", 0.9, "dash")) }
    }

    @Test fun co2MinimumAppliesBeforeAdditionalQuantity() {
        val out = T.run(Co2AgentQuantityCalculator, T.iv("v", 10.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
            T.iv("ftable", 1.0, "kgm3"), T.iv("wmin", 15.0, "kg"), T.iv("addkg", 2.0, "kg"), T.iv("mcyl", 10.0, "kg"))
        // Illustrative inputs to test minimum logic; not a standard table row.
        assertEquals(15.0, out.v("wbasic"))
        assertEquals(17.0, out.v("w"))
        assertEquals(2.0, out.v("cylinders"))
        assertEquals(3.0, out.v("margin"))
    }

    @Test fun contradictoryVolumesCannotSelectCylinders() {
        for (calc in listOf(Fm200AgentQuantityCalculator, Co2AgentQuantityCalculator)) {
            val out = T.run(calc, T.iv("v", 100.0, "m3"), T.iv("vgross", 50.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
                T.iv("s", 0.13782, "m3perkg"), T.iv("c", 7.0, "pct"), T.iv("ftable", 1.0, "kgm3"), T.iv("mcyl", 45.0, "kg"))
            assertTrue(out.results.none { it.id == "cylinders" })
        }
    }

    private fun base() = GOLDEN_CASES.first { it.calculatorId == "duck-foot-bend-base" }.inputs
        .associate { it.inputId to InputValue(it.inputId, it.baseValue, it.displayUnitId) }

    @Test fun upliftSuppressesFullContactPressuresAndThreadAreaIsUsed() {
        val out = DuckFootBendBaseDesignCalculator.run(base() + ("atBolt" to T.iv("atBolt", 1247.0, "mm2")))
        assertTrue(out.v("eccentricity") > out.v("kernRadius"))
        assertTrue(out.results.none { it.id == "bearingMax" || it.id == "bearingMin" })
        assertTrue(out.warnings.any { it.contains("predicts uplift") })
        assertEquals(153.048, out.v("sigmaThread"), 0.01)
        assertTrue(out.v("sigmaThread") > out.v("sigmaTAct"))
    }

    @Test fun fullContactPressureDistributionBalancesAxialLoad() {
        val out = DuckFootBendBaseDesignCalculator.run(base() + ("h" to T.iv("h", 0.1, "m")))
        assertEquals(2.0 * out.v("bearing"), out.v("bearingMax") + out.v("bearingMin"), 1e-10)
        assertTrue(out.v("bearingMin") > 0.0)
        assertFailsWith<ValidationException> {
            DuckFootBendBaseDesignCalculator.run(base() + ("atBolt" to T.iv("atBolt", 10000.0, "mm2")))
        }
    }
}
