package com.mechforge.core

import com.mechforge.core.calcs.BoltTorqueCalculator
import com.mechforge.core.calcs.PumpPowerCalculator
import com.mechforge.core.calcs.ReynoldsNumberCalculator
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Examples whose expected values come from outside this codebase.
 *
 * F36 of the technical review made the point sharply: the independent model re-derives the equations
 * from the same understanding as the engine, so a conceptual misunderstanding is repeated in both and
 * neither notices. The golden suite cannot catch that class of error. These cases can, because their
 * expected values are the equations themselves evaluated by hand on round numbers - anyone can check
 * them with a calculator, and if the engine disagrees the divergence is in the code, not in a mirrored
 * assumption.
 *
 * Each case names the relation it checks and the arithmetic, so a reviewer can reproduce it without
 * running anything.
 */
class TextbookExamplesTest {

    private fun iv(id: String, value: Double, unitId: String) =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    /** T = K F d: 0.2 x 50 kN x 12 mm = 120 N.m exactly. */
    @Test
    fun boltTorqueIsTheHandCalculation() {
        val out = BoltTorqueCalculator.run(
            mapOf(
                "d" to iv("d", 12.0, "mm"),
                "f" to iv("f", 50.0, "kn"),
                "k" to iv("k", 0.2, "dash"),
            ),
        )
        val t = out.results.first { it.id == "t" }.value
        assertEquals(120.0, t, 0.5)
    }

    /** P = rho g Q H: 998.2 x 9.80665 x 0.1 x 20 = 19.58 kW exactly at unit efficiency. */
    @Test
    fun pumpHydraulicPowerIsTheHandCalculation() {
        val out = PumpPowerCalculator.run(
            mapOf(
                "q" to iv("q", 360.0, "m3h"),
                "h" to iv("h", 20.0, "m"),
                "eta" to iv("eta", 1.0, "dash"),
                "rho" to iv("rho", 998.2, "kgm3"),
            ),
        )
        val p = out.results.first { it.id == "hydraulic" }.value
        assertEquals(998.2 * 9.80665 * 0.1 * 20.0 / 1000.0, p, 0.05)
    }

    /** Re = v D / nu: 1 m/s x 0.1 m / 1e-6 = 100 000 exactly. */
    @Test
    fun reynoldsNumberIsTheHandCalculation() {
        val out = ReynoldsNumberCalculator.run(
            mapOf(
                "v" to iv("v", 1.0, "ms"),
                "d" to iv("d", 100.0, "mm"),
                "nu" to iv("nu", 1e-6, "m2s"),
            ),
        )
        val re = out.results.first { it.id == "re" }.value
        assertTrue(Math.abs(re - 100000.0) / 100000.0 < 1e-6, "Re = $re")
    }
}
