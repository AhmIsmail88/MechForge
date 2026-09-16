package com.mechforge.core

import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Engineering review P1-8: a value outside a usual engineering band must be reported, a value
 * inside the band must stay quiet, and an assumed default must be reported exactly once.
 *
 * Every input below is given in the family SI base unit, exactly as the engine stores it.
 */
class EngineeringRangeWarningTest {

    private fun run(calcId: String, vararg inputs: Triple<String, Double, String>): CalcOutput =
        CalculatorRegistry.byIdOrThrow(calcId).run(
            inputs.associate { (id, value, unit) -> id to InputValue(id, value, unit) }
        )

    private fun flags(out: CalcOutput, fragment: String) = out.warnings.any { it.contains(fragment) }

    private fun ductVelocity(q: Double) = run(
        "duct-velocity",
        Triple("q", q, "m3s"),
        Triple("d", 0.3, "mm"),
    )

    private fun ductLoss(v: Double) = run(
        "duct-pressure-loss",
        Triple("v", v, "ms"),
        Triple("l", 10.0, "m"),
        Triple("d", 0.3, "mm"),
    )

    @Test
    fun ductVelocityOutOfBandIsReported() {
        val fast = ductVelocity(0.85) // 12.0 m/s in a 300 mm duct
        assertTrue(flags(fast, "Velocity above 8 m/s"), "12 m/s must be flagged: ${fast.results}")
        val slow = ductVelocity(0.10) // 1.4 m/s
        assertTrue(flags(slow, "Velocity below 2 m/s"), "1.4 m/s must be flagged")
        val normal = ductVelocity(0.35) // 4.95 m/s
        assertTrue(
            normal.warnings.none { it.contains("Velocity above") || it.contains("Velocity below") },
            "4.95 m/s is inside the band but raised ${normal.warnings}",
        )
    }

    @Test
    fun ductFrictionIsCheckedAgainstThePaPerMetreBand() {
        val high = ductLoss(15.0) // about 6.4 Pa/m
        assertTrue(flags(high, "Friction loss above 1.5 Pa/m"), "6 Pa/m must be flagged: ${high.warnings}")
        val normal = ductLoss(6.0) // about 1.2 Pa/m
        assertTrue(
            normal.warnings.none { it.contains("Friction loss above") || it.contains("Friction loss below") },
            "1.2 Pa/m is inside the band but raised ${normal.warnings}",
        )
    }

    @Test
    fun aPinchedTerminalDifferenceIsReported() {
        val pinch = run(
            "lmtd",
            Triple("thin", 31.0 + 273.15, "c"), Triple("thout", 30.0 + 273.15, "c"),
            Triple("tcin", 20.0 + 273.15, "c"), Triple("tcout", 27.0 + 273.15, "c"),
        )
        assertTrue(flags(pinch, "below 5 K"), "a 4 K approach must be flagged: ${pinch.warnings}")
        val roomy = run(
            "lmtd",
            Triple("thin", 90.0 + 273.15, "c"), Triple("thout", 70.0 + 273.15, "c"),
            Triple("tcin", 20.0 + 273.15, "c"), Triple("tcout", 25.0 + 273.15, "c"),
        )
        assertTrue(
            roomy.warnings.none { it.contains("below 5 K") },
            "a 25 K approach is comfortable but raised ${roomy.warnings}",
        )
    }

    @Test
    fun nozzlePressureAndEffectivenessBandsAreChecked() {
        val pascalPerPsi = 6894.757293168361
        val high = run(
            "hose-nozzle-flow",
            Triple("d", 0.0254, "in"), Triple("p", 150.0 * pascalPerPsi, "psi"),
        )
        assertTrue(flags(high, "outside the usual 50-100 psi"), "150 psi must be flagged: ${high.warnings}")
        val normal = run(
            "hose-nozzle-flow",
            Triple("d", 0.0254, "in"), Triple("p", 75.0 * pascalPerPsi, "psi"),
        )
        assertTrue(
            normal.warnings.none { it.contains("outside the usual") },
            "75 psi is the usual band but raised ${normal.warnings}",
        )

        val large = run("hx-effectiveness-ntu", Triple("ntu", 5.0, "dash"), Triple("cr", 0.0, "dash"))
        assertTrue(flags(large, "Effectiveness above 95 %"), "99 % effectiveness must be flagged")
        val moderate = run("hx-effectiveness-ntu", Triple("ntu", 1.0, "dash"), Triple("cr", 0.0, "dash"))
        assertTrue(
            moderate.warnings.none { it.contains("Effectiveness above") },
            "63 % effectiveness raised ${moderate.warnings}",
        )
    }

    @Test
    fun anAssumedDefaultIsReportedExactlyOnce() {
        // the hand-written notes were replaced by InputSpec.assumedWhenOmitted: nothing doubles up
        val hammer = run(
            "water-hammer",
            Triple("dv", 2.0, "ms"), Triple("c", 1000.0, "ms"),
        )
        assertEquals(
            1,
            hammer.warnings.count { it.contains("assumed as 1000 kg/m3") },
            "density caveat duplicated: ${hammer.warnings}",
        )

        val lmtdNoArrangement = run(
            "lmtd",
            Triple("thin", 90.0 + 273.15, "c"), Triple("thout", 70.0 + 273.15, "c"),
            Triple("tcin", 20.0 + 273.15, "c"), Triple("tcout", 25.0 + 273.15, "c"),
        )
        assertEquals(
            1,
            lmtdNoArrangement.warnings.count { it.contains("counter-flow") },
            "arrangement caveat duplicated: ${lmtdNoArrangement.warnings}",
        )
    }
}