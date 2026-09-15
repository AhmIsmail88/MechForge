package com.mechforge.core

import com.mechforge.core.calcs.EquivalentLengthCalculator
import com.mechforge.core.calcs.PipeWallThicknessCalculator
import com.mechforge.core.calcs.PipeWeightCalculator
import com.mechforge.core.calcs.ThermalExpansionCalculator
import com.mechforge.core.calcs.ValveKvCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PipeWeightTest {

    @Test
    fun sixInchSchedule40Case() {
        // 168.3 mm OD, 7.11 mm wall → 28.27 kg/m (classic value)
        val out = T.run(
            PipeWeightCalculator,
            T.iv("od", 168.3, "mm"), T.iv("t", 7.11, "mm"),
        )
        assertEquals(28.27, out.results.first { it.id == "w" }.value, 0.15)
        assertEquals(154.08, out.results.first { it.id == "id" }.value, 0.1)
    }

    @Test
    fun imperialPipeCase() {
        // 6 in OD, 0.28 in wall → ≈25.48 kg/m
        val out = T.run(
            PipeWeightCalculator,
            T.iv("od", 6.0, "in"), T.iv("t", 0.28, "in"),
        )
        assertEquals(25.48, out.results.first { it.id == "w" }.value, 0.2)
    }

    @Test
    fun customDensityCase() {
        // Stainless 8000 kg/m³ → 28.27 × 8000/7850 = 28.81 kg/m
        val out = T.run(
            PipeWeightCalculator,
            T.iv("od", 168.3, "mm"), T.iv("t", 7.11, "mm"), T.iv("rho", 8000.0, "kgm3"),
        )
        assertEquals(28.81, out.results.first { it.id == "w" }.value, 0.15)
    }

    @Test
    fun excessiveWallRejected() {
        try {
            T.run(PipeWeightCalculator, T.iv("od", 100.0, "mm"), T.iv("t", 50.0, "mm"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "t" })
        }
    }
}

class PipeWallThicknessTest {

    @Test
    fun b313FormHandCheck() {
        // p = 1 MPa (10 bar), D = 300 mm, S = 140 MPa, E = 1.0, Y = 0.4, CA = 2 mm, mill 12.5 %
        //   t = p*D/(2*(S*E + p*Y)) = 1.0684 mm ; t + CA = 3.0684 mm ; /0.875 = 3.5067 mm
        val out = T.run(
            PipeWallThicknessCalculator,
            T.iv("p", 10.0, "bar"), T.iv("d", 300.0, "mm"),
            T.iv("sigma", 140.0, "mpa"), T.iv("ca", 2.0, "mm"),
        )
        assertEquals(1.0684, out.results.first { it.id == "tp" }.value, 0.001)
        assertEquals(3.0684, out.results.first { it.id == "t" }.value, 0.005)
        assertEquals(3.5067, out.results.first { it.id == "tNom" }.value, 0.01)
    }

    @Test
    fun weldJointFactorReducesTheAllowable() {
        // E = 0.85 -> t = 1.2563 mm (thicker wall required for a welded joint)
        val out = T.run(
            PipeWallThicknessCalculator,
            T.iv("p", 10.0, "bar"), T.iv("d", 300.0, "mm"),
            T.iv("sigma", 140.0, "mpa"), T.iv("ca", 2.0, "mm"), T.iv("e", 0.85, "dash"),
        )
        assertEquals(1.2563, out.results.first { it.id == "tp" }.value, 0.001)
    }

    @Test
    fun psiAndInchCase() {
        // p = 150 psi, D = 12 in, S = 20000 psi (137.895 MPa), CA = 3 mm
        val out = T.run(
            PipeWallThicknessCalculator,
            T.iv("p", 150.0, "psi"), T.iv("d", 12.0, "in"),
            T.iv("sigma", 137.895, "mpa"), T.iv("ca", 3.0, "mm"),
        )
        assertEquals(1.1396, out.results.first { it.id == "tp" }.value, 0.005)
        assertEquals(4.1396, out.results.first { it.id == "t" }.value, 0.01)
        assertEquals(4.7310, out.results.first { it.id == "tNom" }.value, 0.02)
    }

    @Test
    fun zeroMillToleranceLeavesTheMinimumThickness() {
        val out = T.run(
            PipeWallThicknessCalculator,
            T.iv("p", 10.0, "bar"), T.iv("d", 300.0, "mm"),
            T.iv("sigma", 140.0, "mpa"), T.iv("ca", 2.0, "mm"), T.iv("mill", 0.0, "pct"),
        )
        assertEquals(
            out.results.first { it.id == "t" }.value,
            out.results.first { it.id == "tNom" }.value,
            1e-9,
        )
    }

    @Test
    fun codeWarningIsPresent() {
        val out = T.run(
            PipeWallThicknessCalculator,
            T.iv("p", 10.0, "bar"), T.iv("d", 300.0, "mm"), T.iv("sigma", 140.0, "mpa"),
        )
        assertTrue(out.warnings.any { it.contains("ASME B31.3") })
    }

    @Test
    fun zeroAllowableStressRejected() {
        try {
            T.run(
                PipeWallThicknessCalculator,
                T.iv("p", 10.0, "bar"), T.iv("d", 300.0, "mm"), T.iv("sigma", 0.0, "mpa"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "sigma" })
        }
    }
}

class ThermalExpansionTest {

    @Test
    fun textbookCase() {
        // α = 12 µm/(m·K), L = 30 m, ΔT = 50 K → 18 mm
        val out = T.run(
            ThermalExpansionCalculator,
            T.iv("alpha", 12.0, "permk"), T.iv("l", 30.0, "m"), T.iv("dt", 50.0, "delc"),
        )
        assertEquals(18.0, out.results.first { it.id == "dl" }.value, 0.05)
    }

    @Test
    fun fahrenheitDifferenceCase() {
        // ΔT = 90 °F = 50 K → same 18 mm
        val out = T.run(
            ThermalExpansionCalculator,
            T.iv("alpha", 12.0, "permk"), T.iv("l", 30.0, "m"), T.iv("dt", 90.0, "delf"),
        )
        assertEquals(18.0, out.results.first { it.id == "dl" }.value, 0.05)
    }

    @Test
    fun stainlessValueAndLargeExpansionWarning() {
        // α = 16 µm/(m·K), L = 100 m, ΔT = 80 K → 128 mm (>5 mm/m triggers warning)
        val out = T.run(
            ThermalExpansionCalculator,
            T.iv("alpha", 16.0, "permk"), T.iv("l", 100.0, "m"), T.iv("dt", 80.0, "delc"),
        )
        assertEquals(128.0, out.results.first { it.id == "dl" }.value, 0.5)
        assertTrue(out.warnings.any { it.contains("expansion loops") })
    }
}

class EquivalentLengthTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            EquivalentLengthCalculator,
            T.iv("k", 2.5, "dash"), T.iv("d", 100.0, "mm"), T.iv("f", 0.02, "dash"),
        )
        assertEquals(12.5, out.results.first { it.id == "leq" }.value, 0.02)
        assertEquals(125.0, out.results.first { it.id == "inD" }.value, 0.5)
    }

    @Test
    fun imperialDiameterCase() {
        // K = 10 (open gate valve), D = 6 in, f = 0.015 → L_eq = 10×0.1524/0.015 = 101.6 m
        val out = T.run(
            EquivalentLengthCalculator,
            T.iv("k", 10.0, "dash"), T.iv("d", 6.0, "in"), T.iv("f", 0.015, "dash"),
        )
        assertEquals(101.6, out.results.first { it.id == "leq" }.value, 0.5)
    }

    @Test
    fun zeroFrictionFactorRejected() {
        try {
            T.run(
                EquivalentLengthCalculator,
                T.iv("k", 2.5, "dash"), T.iv("d", 100.0, "mm"), T.iv("f", 0.0, "dash"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "f" })
        }
    }
}

class ValveKvTest {

    @Test
    fun textbookCase() {
        val out = T.run(
            ValveKvCalculator,
            T.iv("kv", 10.0, "dash"), T.iv("dp", 1.0, "bar"), T.iv("sg", 1.0, "dash"),
        )
        assertEquals(10.0, out.results.first { it.id == "q" }.value, 0.01)
        assertEquals(11.56, out.results.first { it.id == "cv" }.value, 0.02)
    }

    @Test
    fun psiAndHigherSpecificGravityCase() {
        // ΔP = 14.5038 psi = 1 bar, SG = 1.2 → Q = 10/√1.2 = 9.129 m³/h
        val out = T.run(
            ValveKvCalculator,
            T.iv("kv", 10.0, "dash"), T.iv("dp", 1.0, "bar"), T.iv("sg", 1.2, "dash"),
        )
        assertEquals(9.129, out.results.first { it.id == "q" }.value, 0.02)
    }

    @Test
    fun defaultSpecificGravityWarns() {
        val out = T.run(ValveKvCalculator, T.iv("kv", 10.0, "dash"), T.iv("dp", 1.0, "bar"))
        assertTrue(out.warnings.any { it.contains("assumed 1.0") })
        assertTrue(out.warnings.any { it.contains("Liquid sizing only") })
    }

    @Test
    fun zeroKvRejected() {
        try {
            T.run(ValveKvCalculator, T.iv("kv", 0.0, "dash"), T.iv("dp", 1.0, "bar"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "kv" })
        }
    }
}
