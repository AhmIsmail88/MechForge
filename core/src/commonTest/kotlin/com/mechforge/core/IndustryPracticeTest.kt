package com.mechforge.core

import com.mechforge.core.calcs.BearingL10Calculator
import com.mechforge.core.calcs.BoltTorqueCalculator
import com.mechforge.core.calcs.ChlorineDoseCalculator
import com.mechforge.core.calcs.DuctVelocityCalculator
import com.mechforge.core.calcs.FanPowerCalculator
import com.mechforge.core.calcs.HoseNozzleFlowCalculator
import com.mechforge.core.calcs.PipeWeightCalculator
import com.mechforge.core.calcs.WaterHammerCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Regression tests for the industry-practice audit of the non-fire calculators.
 *
 * Expected values were computed independently (python), see the audit notes:
 *   duct-velocity, Q = 1 m3/s, D = 400 mm  ->  A = 0.1256637 m2, v = 7.95775 m/s
 *   pipe-weight, OD = 100 mm, t = 5 mm, steel  ->  11.7142 kg/m ; water content 6.3617 kg/m
 *   fan-power, Q = 1 m3/s, dP = 1000 Pa, 70 %/95 %  ->  1.5038 kW motor -> 2.2 kW IEC
 *   water-hammer, steel water pipe D = 100 mm, t = 5 mm  ->  c = 1330.2433 m/s
 */
class DuctVelocityRoundTest {

    @Test
    fun roundDuctComputesAreaFromDiameter() {
        val out = T.run(
            DuctVelocityCalculator,
            T.iv("q", 3600.0, "m3h"), T.iv("d", 400.0, "mm"),
        )
        assertEquals(0.125664, out.results.first { it.id == "a" }.value, 1e-5)
        assertEquals(7.95775, out.results.first { it.id == "v" }.value, 0.001)
    }

    @Test
    fun rectangularDuctStillWorks() {
        val out = T.run(
            DuctVelocityCalculator,
            T.iv("q", 3600.0, "m3h"), T.iv("w", 500.0, "mm"), T.iv("h", 400.0, "mm"),
        )
        // 1 m3/s through 0.5 m x 0.4 m = 0.2 m2 -> 5 m/s
        assertEquals(5.0, out.results.first { it.id == "v" }.value, 1e-9)
    }

    @Test
    fun roundAndRectangularAreMutuallyExclusive() {
        try {
            T.run(DuctVelocityCalculator, T.iv("q", 3600.0, "m3h"))
            fail("expected ValidationException when no duct shape is given")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
        try {
            T.run(
                DuctVelocityCalculator,
                T.iv("q", 3600.0, "m3h"), T.iv("d", 400.0, "mm"),
                T.iv("w", 500.0, "mm"), T.iv("h", 400.0, "mm"),
            )
            fail("expected ValidationException when the diameter and a width are both given")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }
}

class PipeWeightContentTest {

    @Test
    fun waterContentGivesTheOperatingWeight() {
        val out = T.run(
            PipeWeightCalculator,
            T.iv("od", 100.0, "mm"), T.iv("t", 5.0, "mm"), T.iv("rhoc", 1000.0, "kgm3"),
        )
        assertEquals(11.7142, out.results.first { it.id == "w" }.value, 0.005)
        assertEquals(90.0, out.results.first { it.id == "id" }.value, 0.1)
        assertEquals(6.3617, out.results.first { it.id == "wc" }.value, 0.005)
        assertEquals(18.0759, out.results.first { it.id == "wtot" }.value, 0.01)
    }

    @Test
    fun withoutContentOnlyThePipeMassIsReported() {
        val out = T.run(
            PipeWeightCalculator,
            T.iv("od", 100.0, "mm"), T.iv("t", 5.0, "mm"),
        )
        assertTrue(out.results.none { it.id == "wc" })
        assertTrue(out.results.none { it.id == "wtot" })
        assertEquals(11.7142, out.results.first { it.id == "w" }.value, 0.005)
    }
}

class FanPowerMotorRatingTest {

    @Test
    fun nextStandardIecRatingIsSelected() {
        val out = T.run(
            FanPowerCalculator,
            T.iv("q", 3600.0, "m3h"), T.iv("dp", 1000.0, "pa"),
            T.iv("etaf", 70.0, "pct"), T.iv("etad", 95.0, "pct"),
        )
        assertEquals(1.5038, out.results.first { it.id == "pmotor" }.value, 0.005)
        assertEquals(2.2, out.results.first { it.id == "motor" }.value, 1e-9)
        assertTrue(out.warnings.any { it.contains("IEC") })
    }

    @Test
    fun motorAtAnExactRatingIsKept() {
        // 1 m3/s at 700 Pa with 100 % fan efficiency = 700 W = 0.7 kW -> 0.75 kW rating
        val out = T.run(
            FanPowerCalculator,
            T.iv("q", 3600.0, "m3h"), T.iv("dp", 700.0, "pa"), T.iv("etaf", 100.0, "pct"),
        )
        assertEquals(0.75, out.results.first { it.id == "motor" }.value, 1e-9)
    }
}

class HoseNozzleCoefficientTest {

    @Test
    fun defaultCoefficientKeepsTheStandardValue() {
        val out = T.run(
            HoseNozzleFlowCalculator,
            T.iv("d", 1.5, "in"), T.iv("p", 100.0, "psi"),
        )
        assertEquals(668.25, out.results.first { it.id == "q" }.value, 0.05)
    }

    @Test
    fun nozzleCoefficientScalesTheFlow() {
        val out = T.run(
            HoseNozzleFlowCalculator,
            T.iv("d", 1.5, "in"), T.iv("p", 100.0, "psi"), T.iv("c", 0.98, "dash"),
        )
        assertEquals(654.885, out.results.first { it.id == "q" }.value, 0.05)
    }
}

class WaterHammerWaveSpeedTest {

    @Test
    fun waveSpeedIsCalculatedFromThePipeData() {
        // water: K = 2.15 GPa, rho = 1000 kg/m3 ; steel: E = 200 GPa ; D = 100 mm, t = 5 mm
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("dv", 2.0, "ms"),
            T.iv("d", 100.0, "mm"), T.iv("t", 5.0, "mm"),
        )
        assertEquals(1330.2433, out.results.first { it.id == "cUsed" }.value, 0.05)
        assertEquals(26.6049, out.results.first { it.id == "dp" }.value, 0.02)
        assertEquals(271.294, out.results.first { it.id == "head" }.value, 0.2)
    }

    @Test
    fun plasticPipeGivesAMuchLowerWaveSpeed() {
        // E = 3 GPa (thermoplastic), D = 110 mm, t = 10 mm -> c = 491.96 m/s
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("dv", 2.0, "ms"),
            T.iv("d", 110.0, "mm"), T.iv("t", 10.0, "mm"), T.iv("epipe", 3000.0, "mpa"),
        )
        assertEquals(491.9617, out.results.first { it.id == "cUsed" }.value, 0.05)
    }

    @Test
    fun enteredWaveSpeedStillWins() {
        val out = T.run(
            WaterHammerCalculator,
            T.iv("rho", 1000.0, "kgm3"), T.iv("dv", 2.0, "ms"),
            T.iv("c", 1200.0, "ms"), T.iv("d", 100.0, "mm"), T.iv("t", 5.0, "mm"),
        )
        assertEquals(1200.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(24.0, out.results.first { it.id == "dp" }.value, 0.01)
    }

    @Test
    fun missingWaveSpeedAndPipeDataIsAnError() {
        try {
            T.run(
                WaterHammerCalculator,
                T.iv("rho", 1000.0, "kgm3"), T.iv("dv", 2.0, "ms"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "c" })
        }
    }
}

class ChlorineDoseDisplayTest {

    @Test
    fun doseAndMassRateAreConsistent() {
        // 5000 m3/d at 4 mg/L -> 20 kg/d of pure chlorine
        val out = T.run(
            ChlorineDoseCalculator,
            T.iv("q", 5000.0, "m3d"), T.iv("dose", 4.0, "mgl"),
        )
        assertEquals(20.0, out.results.first { it.id == "mr" }.value, 0.01)
        // the step must echo the dose back in mg/L (1 kg/m3 = 1000 mg/L)
        assertTrue(out.steps.any { it.contains("4.000") && it.contains("mg/L") }, out.steps.toString())
    }
}

/**
 * Bolt preload design from the property class (ISO 898-1 proof strengths) and the metric
 * tensile stress area, plus the reliability/lubrication adjusted bearing life.
 *
 * Independently computed (python):
 *   M12, p = 1.75 mm -> A_t = 84.266 mm2 (tabulated 84.3)
 *     8.8, 65 %      -> F = 35.055 kN, T = 84.13 N.m
 *   M16, p = 2.0 mm  -> A_t = 156.668 mm2 (tabulated 157)
 *     10.9, 65 %     -> F = 95.724 kN, T = 306.32 N.m
 *   bearing C = 30 kN, P = 5 kN, p = 3, n = 1500 rpm
 *     L10 = 216 x 10^6 rev = 2400 h ; a1 = 0.62 -> 133.92 x 10^6 rev (1488 h)
 */
class BoltPreloadDesignTest {

    @Test
    fun m12Class88DesignPreloadAndTorque() {
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("d", 12.0, "mm"), T.iv("pitch", 1.75, "mm"),
            T.iv("class", 1.0, "dash"), T.iv("preloadpct", 65.0, "pct"),
        )
        assertEquals(84.266, out.results.first { it.id == "at" }.value, 0.01)
        assertEquals(35.0547, out.results.first { it.id == "fRec" }.value, 0.01)
        assertEquals(84.13, out.results.first { it.id == "tRec" }.value, 0.05)
    }

    @Test
    fun m16Class109DesignPreloadAndTorque() {
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("d", 16.0, "mm"), T.iv("pitch", 2.0, "mm"),
            T.iv("class", 2.0, "dash"), T.iv("preloadpct", 65.0, "pct"),
        )
        assertEquals(156.668, out.results.first { it.id == "at" }.value, 0.01)
        assertEquals(95.7238, out.results.first { it.id == "fRec" }.value, 0.01)
        assertEquals(306.32, out.results.first { it.id == "tRec" }.value, 0.1)
    }

    @Test
    fun enteredPreloadIsCheckedAgainstTheProofLoad() {
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("d", 12.0, "mm"), T.iv("pitch", 1.75, "mm"),
            T.iv("class", 1.0, "dash"), T.iv("f", 35.0, "kn"),
        )
        assertEquals(64.9, out.results.first { it.id == "util" }.value, 0.05)
        assertEquals(84.0, out.results.first { it.id == "t" }.value, 0.05)
        assertEquals(415.3, out.results.first { it.id == "sigma" }.value, 1.0)
    }

    @Test
    fun propertyClassWithoutStressAreaIsRejected() {
        try {
            T.run(BoltTorqueCalculator, T.iv("d", 12.0, "mm"), T.iv("class", 1.0, "dash"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "at" })
        }
    }

    @Test
    fun neitherForceNorPropertyClassIsRejected() {
        try {
            T.run(BoltTorqueCalculator, T.iv("d", 12.0, "mm"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "f" })
        }
    }
}

class BearingModifiedLifeTest {

    @Test
    fun basicLifeIsUnchangedWithoutAdjustmentFactors() {
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("exp", 3.0, "dash"),
            T.iv("n", 1500.0, "rpm"),
        )
        assertEquals(216.0e6, out.results.first { it.id == "l10" }.value, 1.0)
        assertEquals(2400.0, out.results.first { it.id == "l10h" }.value, 1.0)
        assertTrue(out.results.none { it.id == "lnm" })
    }

    @Test
    fun reliabilityFactorShrinksTheLife() {
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("exp", 3.0, "dash"),
            T.iv("n", 1500.0, "rpm"), T.iv("rel", 1.0, "dash"),
        )
        assertEquals(0.62, out.results.first { it.id == "a1" }.value, 1e-9)
        assertEquals(133.92e6, out.results.first { it.id == "lnm" }.value, 1.0)
        assertEquals(1488.0, out.results.first { it.id == "lnmh" }.value, 1.0)
    }

    @Test
    fun lubricationFactorScalesTheLife() {
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("exp", 3.0, "dash"),
            T.iv("n", 1500.0, "rpm"), T.iv("aiso", 0.8, "dash"),
        )
        assertEquals(172.8e6, out.results.first { it.id == "lnm" }.value, 1.0)
        assertEquals(1920.0, out.results.first { it.id == "lnmh" }.value, 1.0)
    }

    @Test
    fun bothFactorsApplyTogether() {
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("exp", 3.0, "dash"),
            T.iv("n", 1500.0, "rpm"), T.iv("rel", 4.0, "dash"), T.iv("aiso", 0.8, "dash"),
        )
        assertEquals(0.33, out.results.first { it.id == "a1" }.value, 1e-9)
        assertEquals(57.024e6, out.results.first { it.id == "lnm" }.value, 1.0)
        assertEquals(633.6, out.results.first { it.id == "lnmh" }.value, 1.0)
    }
}