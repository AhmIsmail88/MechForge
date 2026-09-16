package com.mechforge.core

import com.mechforge.core.calcs.BeamCantileverPointCalculator
import com.mechforge.core.calcs.BeamSsUdlCalculator
import com.mechforge.core.calcs.BearingL10Calculator
import com.mechforge.core.calcs.BoltTorqueCalculator
import com.mechforge.core.calcs.GearRatioCalculator
import com.mechforge.core.calcs.SpringRateCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class BoltTorqueTest {

    @Test
    fun torqueFromPreload() {
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("k", 0.2, "dash"), T.iv("d", 16.0, "mm"), T.iv("f", 50.0, "kn"),
        )
        assertEquals(160.0, out.results.first { it.id == "t" }.value, 0.01)
        assertEquals(50.0, out.results.first { it.id == "f" }.value, 0.01)
    }

    @Test
    fun preloadFromTorque() {
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("k", 0.2, "dash"), T.iv("d", 16.0, "mm"), T.iv("t", 160.0, "nm"),
        )
        assertEquals(50.0, out.results.first { it.id == "f" }.value, 0.01)
    }

    @Test
    fun tensileStressWithStressArea() {
        // A_t = 157 mm² for M16 → σ = 50000/157e-6 = 318.5 MPa
        val out = T.run(
            BoltTorqueCalculator,
            T.iv("k", 0.2, "dash"), T.iv("d", 16.0, "mm"), T.iv("f", 50.0, "kn"), T.iv("at", 157.0, "mm2"),
        )
        assertEquals(318.5, out.results.first { it.id == "sigma" }.value, 0.5)
    }

    @Test
    fun bothOrNeitherRejected() {
        try {
            T.run(BoltTorqueCalculator, T.iv("k", 0.2, "dash"), T.iv("d", 16.0, "mm"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
        try {
            T.run(
                BoltTorqueCalculator,
                T.iv("k", 0.2, "dash"), T.iv("d", 16.0, "mm"),
                T.iv("f", 50.0, "kn"), T.iv("t", 160.0, "nm"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
    }
}

class BearingL10Test {

    @Test
    fun textbookCase() {
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("n", 1500.0, "rpm"),
        )
        assertEquals(216e6, out.results.first { it.id == "l10" }.value, 1e6)
        assertEquals(2400.0, out.results.first { it.id == "l10h" }.value, 10.0)
    }

    @Test
    fun rollerExponentCase() {
        // p = 10/3 with C/P = 6 → 6^3.3333 = 380.2 → 380.2e6 revolutions
        val out = T.run(
            BearingL10Calculator,
            T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"), T.iv("exp", 10.0 / 3.0, "dash"),
        )
        assertEquals(392.5e6, out.results.first { it.id == "l10" }.value, 1e6)
    }

    @Test
    fun missingSpeedWarns() {
        val out = T.run(BearingL10Calculator, T.iv("c", 30.0, "kn"), T.iv("p", 5.0, "kn"))
        assertTrue(out.warnings.any { it.contains("Life exponent assumed as 3.0") })
        assertTrue(out.warnings.any { it.contains("hours") })
    }

    @Test
    fun zeroLoadRejected() {
        try {
            T.run(BearingL10Calculator, T.iv("c", 30.0, "kn"), T.iv("p", 0.0, "kn"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "p" })
        }
    }
}

class SpringRateTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            SpringRateCalculator,
            T.iv("d", 2.0, "mm"), T.iv("dm", 20.0, "mm"), T.iv("n", 10.0, "dash"),
        )
        assertEquals(1982.5, out.results.first { it.id == "k" }.value, 10.0)
        assertEquals(1.9825, out.results.first { it.id == "kmm" }.value, 0.02)
    }

    @Test
    fun deflectionUnderLoad() {
        val out = T.run(
            SpringRateCalculator,
            T.iv("d", 2.0, "mm"), T.iv("dm", 20.0, "mm"), T.iv("n", 10.0, "dash"), T.iv("f", 10.0, "n"),
        )
        assertEquals(5.044, out.results.first { it.id == "delta" }.value, 0.05)
    }

    @Test
    fun indexOutsideRangeWarns() {
        // D/d = 8.5 is fine; use D=8 mm, d=2 mm → index 4 (boundary) — use D=7,d=2 → 3.5
        val out = T.run(
            SpringRateCalculator,
            T.iv("d", 2.0, "mm"), T.iv("dm", 7.0, "mm"), T.iv("n", 10.0, "dash"),
        )
        assertTrue(out.warnings.any { it.contains("Spring index") })
    }

    @Test
    fun meanDiameterNotLargerRejected() {
        try {
            T.run(
                SpringRateCalculator,
                T.iv("d", 20.0, "mm"), T.iv("dm", 20.0, "mm"), T.iv("n", 10.0, "dash"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "dm" })
        }
    }
}

class BeamSsUdlTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            BeamSsUdlCalculator,
            T.iv("w", 10.0, "knlperm"), T.iv("l", 6.0, "m"),
            T.iv("e", 200000.0, "mpa"), T.iv("i", 1e8, "mm4"), T.iv("z", 5e5, "mm3sect"),
        )
        assertEquals(8.4375, out.results.first { it.id == "defl" }.value, 0.05)
        assertEquals(90.0, out.results.first { it.id == "sigma" }.value, 0.5)
        assertEquals(45000.0, out.results.first { it.id == "moment" }.value, 100.0)
    }

    @Test
    fun imperialLineLoadCase() {
        // 1 kip/ft = 14.5939029 kN/m, L = 20 ft = 6.096 m, E = 29e6 psi = 199948 MPa, I = 100 in⁴ = 4.16231e-5 m⁴
        val out = T.run(
            BeamSsUdlCalculator,
            T.iv("w", 1000.0, "lbfperft"), T.iv("l", 20.0, "ft"),
            T.iv("e", 200000.0, "mpa"), T.iv("i", 100.0, "in4"),
        )
        // δ = 5wL⁴/(384EI) = 5×14593.9×6.096⁴/(384×2e11×4.162314e-5) = 0.03152 m = 31.52 mm
        assertEquals(31.52, out.results.first { it.id == "defl" }.value, 0.3)
    }

    @Test
    fun missingSectionModulusWarns() {
        val out = T.run(
            BeamSsUdlCalculator,
            T.iv("w", 10.0, "knlperm"), T.iv("l", 6.0, "m"),
            T.iv("e", 200000.0, "mpa"), T.iv("i", 1e8, "mm4"),
        )
        assertTrue(out.warnings.any { it.contains("Section modulus") })
    }
}

class BeamCantileverTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            BeamCantileverPointCalculator,
            T.iv("p", 5.0, "kn"), T.iv("l", 2.0, "m"),
            T.iv("e", 200000.0, "mpa"), T.iv("i", 2e7, "mm4"),
        )
        assertEquals(3.333, out.results.first { it.id == "defl" }.value, 0.02)
        assertEquals(10000.0, out.results.first { it.id == "moment" }.value, 10.0)
    }

    @Test
    fun imperialForceCase() {
        // 1000 lbf = 4448.22 N, L = 1 m, E = 2e11, I = 1e7 mm⁴ → δ = 4448.22/(3×2e11×1e-5) = 7.41e-4 m
        val out = T.run(
            BeamCantileverPointCalculator,
            T.iv("p", 1000.0, "lbf"), T.iv("l", 1.0, "m"),
            T.iv("e", 200000.0, "mpa"), T.iv("i", 1e7, "mm4"),
        )
        assertEquals(0.741, out.results.first { it.id == "defl" }.value, 0.02)
    }

    @Test
    fun zeroSpanRejected() {
        try {
            T.run(
                BeamCantileverPointCalculator,
                T.iv("p", 5.0, "kn"), T.iv("l", 0.0, "m"),
                T.iv("e", 200000.0, "mpa"), T.iv("i", 1e7, "mm4"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "l" })
        }
    }
}

class GearRatioTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            GearRatioCalculator,
            T.iv("z1", 20.0, "dash"), T.iv("z2", 60.0, "dash"),
            T.iv("n1", 1500.0, "rpm"), T.iv("t1", 50.0, "nm"), T.iv("m", 2.0, "mm"),
        )
        assertEquals(3.0, out.results.first { it.id == "i" }.value, 1e-9)
        assertEquals(500.0, out.results.first { it.id == "n2" }.value, 0.01)
        assertEquals(150.0, out.results.first { it.id == "t2" }.value, 0.01)
        assertEquals(40.0, out.results.first { it.id == "d1" }.value, 0.01)
        assertEquals(120.0, out.results.first { it.id == "d2" }.value, 0.01)
    }

    @Test
    fun reductionWithImperialModule() {
        // 1/8 in module (3.175 mm), z1 = 18, z2 = 54 → d1 = 57.15 mm
        val out = T.run(
            GearRatioCalculator,
            T.iv("z1", 18.0, "dash"), T.iv("z2", 54.0, "dash"), T.iv("m", 0.125, "in"),
        )
        assertEquals(57.15, out.results.first { it.id == "d1" }.value, 0.05)
    }

    @Test
    fun highRatioWarns() {
        val out = T.run(GearRatioCalculator, T.iv("z1", 12.0, "dash"), T.iv("z2", 120.0, "dash"))
        assertTrue(out.warnings.any { it.contains("two-stage") })
    }
}
