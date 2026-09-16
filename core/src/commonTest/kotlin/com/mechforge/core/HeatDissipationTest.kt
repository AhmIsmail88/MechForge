package com.mechforge.core

import com.mechforge.core.calcs.HeatDissipationCalculator
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Heat-dissipation airflow: the sensible-heat equation rearranged for the fan capacity, plus the
 * fan-capacity and fan-count fields the review asked for.
 */
class HeatDissipationTest {

    private fun iv(id: String, value: Double, unit: String) =
        InputValue(id, Units.byId(unit).toBase(value), unit)

    private fun run(vararg inputs: InputValue): CalcOutput =
        HeatDissipationCalculator.run(inputs.associateBy { it.inputId })

    private fun valueOf(out: CalcOutput, id: String) = out.results.first { it.id == id }.value

    @Test
    fun textbookCase() {
        // 10 kW removed by air allowed to rise 10 K:
        // m_dot = 10000 / (1005 * 10) = 0.99502 kg/s ;  Q = m_dot/1.2 = 0.82919 m3/s = 2985.07 m3/h
        val out = run(
            iv("p", 10.0, "kw"),
            iv("dt", 10.0, "delk"),
            iv("rho", 1.2, "kgm3"),
            iv("cp", 1005.0, "jkgk"),
        )
        assertEquals(0.9950248756218906, valueOf(out, "mdot"), 1e-12)
        assertEquals(2985.0746268656718, valueOf(out, "q"), 1e-6)
        // the imperial result is the same airflow in CFM, from the engine's own constant
        assertEquals(valueOf(out, "q") / 3600.0 / 4.719474432e-4, valueOf(out, "qCfm"), 1e-6)
        assertEquals(829.1874, valueOf(out, "qLs"), 1e-3)
    }

    @Test
    fun unitConversionGivesTheSameAirflow() {
        // 1 hp = 745.69987158227 W and 18 dF = 10 K, so this is the same duty as the textbook case
        val out = run(
            iv("p", 10.0 / 0.74569987158227 * 0.74569987158227, "kw"),
            iv("dt", 18.0, "delf"),
            iv("rho", 1.2, "kgm3"),
            iv("cp", 1005.0, "jkgk"),
        )
        assertEquals(2985.0746268656718, valueOf(out, "q"), 1e-6)

        val btuh = 10_000.0 / 0.29307107017 // 10 kW expressed in BTU/h (1 kW = 3412.142)
        val out2 = run(
            iv("p", btuh, "btuh"),
            iv("dt", 10.0, "delc"),
            iv("rho", 1.2, "kgm3"),
            iv("cp", 1005.0, "jkgk"),
        )
        assertEquals(2985.0746268656718, valueOf(out2, "q"), 1e-6)

        // kJ/(kg.K) is the same number as J/(kg.K) apart from the factor of 1000
        val out3 = run(
            iv("p", 10.0, "kw"),
            iv("dt", 10.0, "delk"),
            iv("rho", 1.2, "kgm3"),
            iv("cp", 1.005, "kjkgk"),
        )
        assertEquals(2985.0746268656718, valueOf(out3, "q"), 1e-6)
    }

    @Test
    fun omittedAirPropertiesAreReportedAsAssumptions() {
        val out = run(iv("p", 10.0, "kw"), iv("dt", 10.0, "delk"))
        assertEquals(1.2, valueOf(out, "densityUsed"), 1e-12)
        assertEquals(1005.0, valueOf(out, "cpUsed"), 1e-12)
        assertEquals(
            2,
            out.warnings.count { it.contains("assumed as") },
            "both omitted properties must announce themselves: ${out.warnings}",
        )
    }

    @Test
    fun aHotterAllowableRiseNeedsLessAir() {
        val tight = run(iv("p", 10.0, "kw"), iv("dt", 5.0, "delk"))
        val loose = run(iv("p", 10.0, "kw"), iv("dt", 10.0, "delk"))
        assertEquals(
            2.0,
            valueOf(tight, "q") / valueOf(loose, "q"),
            1e-9,
            "airflow is inversely proportional to the allowable rise",
        )
        assertTrue(tight.warnings.any { it.contains("inversely proportional") })
    }

    @Test
    fun fanCapacityAndCountAreSizedAgainstTheResult() {
        // 2985.07 m3/h required, fans of 1000 m3/h each: three are needed
        val twoFans = run(
            iv("p", 10.0, "kw"),
            iv("dt", 10.0, "delk"),
            iv("fancap", 1000.0, "m3h"),
            iv("nfans", 2.0, "dash"),
        )
        assertEquals(3.0, valueOf(twoFans, "fansNeeded"), 1e-9)
        assertEquals(2000.0, valueOf(twoFans, "fanTotal"), 1e-6)
        assertEquals((2000.0 / 2985.0746268656718 - 1.0) * 100.0, valueOf(twoFans, "fanMargin"), 1e-9)
        assertTrue(
            twoFans.warnings.any { it.contains("short of the calculated requirement") },
            "two fans cannot cover the duty: ${twoFans.warnings}",
        )
        assertTrue(
            twoFans.warnings.any { it.contains("3 fan(s) of that capacity are needed") },
            "${twoFans.warnings}",
        )

        val threeFans = run(
            iv("p", 10.0, "kw"),
            iv("dt", 10.0, "delk"),
            iv("fancap", 1000.0, "m3h"),
            iv("nfans", 3.0, "dash"),
        )
        assertEquals(3000.0, valueOf(threeFans, "fanTotal"), 1e-6)
        assertTrue(
            threeFans.warnings.none { it.contains("short of the calculated requirement") },
            "three fans cover the duty: ${threeFans.warnings}",
        )
    }

    @Test
    fun aZeroRiseIsRejected() {
        try {
            run(iv("p", 10.0, "kw"), iv("dt", 0.0, "delk"))
            fail("a zero temperature rise must be rejected")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "dt" }, "${e.errors}")
        }
    }
}
