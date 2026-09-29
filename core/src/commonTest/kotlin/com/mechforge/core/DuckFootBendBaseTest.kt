package com.mechforge.core

import com.mechforge.core.calcs.DuckFootBendBaseDesignCalculator as Calc
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.units.Units
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Duck foot bend base design (plate, ribs and anchor bolts).
 *
 * The expected numbers below were derived by hand from the published equations in the design note
 * (independently of this engine), for the note's own default data set:
 * D = 900 mm, t_elbow = 10 mm, t_pipe = 10 mm, L = 4.10 m, Q = 1320 L/s, P_pump = 9 bar,
 * P_des = 16 bar, Fy = 235 MPa, FOS = 1.67, rho_w = 1000 kg/m3, gamma_s = 7850 kg/m3,
 * H = 0.90 m, W_elbow = 300 kg, D_plate = 1900 mm, n_ribs = 8, h_rib = 300 mm, n_bolts = 12,
 * BCD = 1600 mm, sigma_bolt = 140 MPa, t_plate_sel = 48 mm, t_rib_sel = 32 mm, d_bolt_sel = 48 mm,
 * edge_sel = 100 mm.
 */
class DuckFootBendBaseTest {

    private fun input(id: String, value: Double, unitId: String): InputValue =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    private fun defaults(): Map<String, InputValue> = mapOf(
        "d" to input("d", 900.0, "mm"),
        "tElbow" to input("tElbow", 10.0, "mm"),
        "tPipe" to input("tPipe", 10.0, "mm"),
        "l" to input("l", 4.10, "m"),
        "q" to input("q", 1320.0, "ls"),
        "pPump" to input("pPump", 9.0, "bar"),
        "pDes" to input("pDes", 16.0, "bar"),
        "fy" to input("fy", 235.0, "mpa"),
        "fos" to input("fos", 1.67, "dash"),
        "rhoW" to input("rhoW", 1000.0, "kgm3"),
        "gammaS" to input("gammaS", 7850.0, "kgm3"),
        "h" to input("h", 0.90, "m"),
        "wElbow" to input("wElbow", 300.0, "kg"),
        "dPlate" to input("dPlate", 1900.0, "mm"),
        "nRibs" to input("nRibs", 8.0, "dash"),
        "hRib" to input("hRib", 300.0, "mm"),
        "nBolts" to input("nBolts", 12.0, "dash"),
        "bcd" to input("bcd", 1600.0, "mm"),
        "sigmaBolt" to input("sigmaBolt", 140.0, "mpa"),
        "tPlateSel" to input("tPlateSel", 48.0, "mm"),
        "tRibSel" to input("tRibSel", 32.0, "mm"),
        "dBoltSel" to input("dBoltSel", 48.0, "mm"),
        "edgeSel" to input("edgeSel", 100.0, "mm"),
    )

    private fun value(inputs: Map<String, InputValue>, id: String): Double =
        Calc.run(inputs).results.first { it.id == id }.value

    private fun rel(actual: Double, expected: Double) = abs(actual - expected) / abs(expected)

    @Test
    fun textbookCaseReproducesTheHandCalculation() {
        val out = Calc.run(defaults())

        // Hand calculation from the design note's equations.
        assertTrue(rel(value(defaults(), "v"), 2.074909) < 1e-5, "velocity")
        assertTrue(rel(value(defaults(), "rGov"), 1439.4941) < 1e-5, "governing resultant, kN")
        assertTrue(rel(value(defaults(), "nTotal"), 1055.4201) < 1e-5, "total vertical load, kN")
        assertTrue(rel(value(defaults(), "hTotal"), 1017.876) < 1e-5, "horizontal load, kN")
        assertTrue(rel(value(defaults(), "moment"), 916088.4178) < 1e-5, "overturning moment, N.m")
        assertTrue(rel(value(defaults(), "bearing"), 372.2445) < 1e-5, "bearing stress, kPa")
        assertTrue(rel(value(defaults(), "cantilever"), 500.0) < 1e-9, "plate cantilever, mm")
        assertTrue(rel(value(defaults(), "tPlateReq"), 44.5401) < 1e-4, "required plate thickness, mm")
        assertTrue(rel(value(defaults(), "tRibReq"), 31.2484) < 1e-4, "required rib thickness, mm")
        assertTrue(rel(value(defaults(), "tMax"), 190.8518) < 1e-4, "bolt tension, kN")
        assertTrue(rel(value(defaults(), "vBolt"), 84.823) < 1e-4, "bolt shear, kN")
        assertTrue(rel(value(defaults(), "dBoltReq"), 41.6619) < 1e-4, "required bolt diameter, mm")
        assertTrue(rel(value(defaults(), "sigmaTAct"), 105.4688) < 1e-4, "bolt tensile stress, MPa")
        assertTrue(rel(value(defaults(), "tauAct"), 46.875) < 1e-4, "bolt shear stress, MPa")
        assertTrue(rel(value(defaults(), "interaction"), 0.937517) < 1e-4, "bolt interaction")
        assertTrue(rel(value(defaults(), "dPlateMin"), 1800.0) < 1e-9, "minimum plate diameter, mm")

        // All four checks pass with the note's selected sizes.
        assertEquals(1.0, value(defaults(), "checkPlate"))
        assertEquals(1.0, value(defaults(), "checkRib"))
        assertEquals(1.0, value(defaults(), "checkBolt"))
        assertEquals(1.0, value(defaults(), "checkPlateDia"))
        assertTrue(out.steps.size >= 10, "the working should be shown")
    }

    @Test
    fun theSamePhysicalCaseInOtherUnitsGivesTheSameAnswers() {
        val metric = defaults()
        val imperial = mapOf(
            "d" to input("d", 900.0 / 25.4, "in"),
            "tElbow" to input("tElbow", 10.0 / 25.4, "in"),
            "tPipe" to input("tPipe", 10.0 / 25.4, "in"),
            "l" to input("l", 4.10 / 0.3048, "ft"),
            "q" to input("q", 1320.0 / 3.785411784 * 60.0, "gpm"),
            "pPump" to input("pPump", 9.0 * 1e5 / 6894.757293168361, "psi"),
            "pDes" to input("pDes", 16.0 * 1e5 / 6894.757293168361, "psi"),
            "fy" to input("fy", 235.0, "mpa"),
            "fos" to input("fos", 1.67, "dash"),
            "rhoW" to input("rhoW", 1000.0 / 16.018463373960142, "lbft3"),
            "gammaS" to input("gammaS", 7850.0 / 16.018463373960142, "lbft3"),
            "h" to input("h", 0.90 / 0.3048, "ft"),
            "wElbow" to input("wElbow", 300.0, "kg"),
            "dPlate" to input("dPlate", 1900.0 / 25.4, "in"),
            "nRibs" to input("nRibs", 8.0, "dash"),
            "hRib" to input("hRib", 300.0 / 25.4, "in"),
            "nBolts" to input("nBolts", 12.0, "dash"),
            "bcd" to input("bcd", 1600.0 / 25.4, "in"),
            "sigmaBolt" to input("sigmaBolt", 140.0, "mpa"),
            "tPlateSel" to input("tPlateSel", 48.0 / 25.4, "in"),
            "tRibSel" to input("tRibSel", 32.0 / 25.4, "in"),
            "dBoltSel" to input("dBoltSel", 48.0 / 25.4, "in"),
            "edgeSel" to input("edgeSel", 100.0 / 25.4, "in"),
        )
        for (id in listOf("v", "rGov", "nTotal", "moment", "tPlateReq", "tRibReq", "interaction")) {
            assertTrue(
                rel(value(imperial, id), value(metric, id)) < 1e-6,
                "$id differs between unit systems",
            )
        }
        assertEquals(1.0, value(imperial, "checkPlate"))
        assertEquals(1.0, value(imperial, "checkBolt"))
    }

    @Test
    fun aThinBasePlateFailsItsCheckAndSaysSo() {
        val thin = defaults() + ("tPlateSel" to input("tPlateSel", 20.0, "mm"))
        val out = Calc.run(thin)
        assertEquals(0.0, out.results.first { it.id == "checkPlate" }.value)
        assertTrue(
            out.warnings.any { it.contains("Base plate thickness NOT sufficient") },
            "a failing plate check must state what to increase",
        )
        assertTrue(
            out.results.first { it.id == "tPlateMargin" }.value < 0.0,
            "the margin should be negative when the check fails",
        )
    }

    @Test
    fun aPlateTooSmallForTheBoltCircleFailsTheFitCheck() {
        val small = defaults() + ("dPlate" to input("dPlate", 1700.0, "mm"))
        val out = Calc.run(small)
        assertEquals(0.0, out.results.first { it.id == "checkPlateDia" }.value)
        assertTrue(out.warnings.any { it.contains("Plate diameter NOT sufficient") })
    }

    @Test
    fun theDesignPressureCaseGoverningIsReportedInTheSteps() {
        // With the pump pressure raised above the design pressure the operating case must govern.
        val high = defaults() + ("pPump" to input("pPump", 20.0, "bar"))
        val out = Calc.run(high)
        val text = (out.steps + out.warnings).joinToString(" ")
        assertTrue(
            text.contains("operating"),
            "the result must name which load case governed",
        )
    }
}
