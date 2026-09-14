package com.mechforge.core

import com.mechforge.core.calcs.IdealGasCalculator
import com.mechforge.core.calcs.PowerTorqueRpmCalculator
import com.mechforge.core.calcs.TorsionalStressCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class IdealGasTest {

    @Test
    fun airDensityTextbook() {
        // Air at 101.325 kPa and 20 °C → ρ ≈ 1.2041 kg/m³
        val out = T.run(
            IdealGasCalculator,
            T.iv("p", 101.325, "kpa"), T.iv("t", 20.0, "c"), T.iv("m", 28.965, "gmol"),
        )
        assertEquals(1.2041, out.results.first { it.id == "rho" }.value, 0.005)
    }

    @Test
    fun massForVolume() {
        val out = T.run(
            IdealGasCalculator,
            T.iv("p", 101.325, "kpa"), T.iv("t", 20.0, "c"), T.iv("m", 28.965, "gmol"), T.iv("v", 1.0, "m3"),
        )
        assertEquals(1.2041, out.results.first { it.id == "mass" }.value, 0.005)
    }

    @Test
    fun belowAbsoluteZeroRejected() {
        try {
            T.run(IdealGasCalculator, T.iv("p", 101.325, "kpa"), T.iv("t", -300.0, "c"), T.iv("m", 28.965, "gmol"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "t" })
        }
    }

    @Test
    fun stpAirCheck() {
        // 1 atm, 0 °C → ρ = 1.293 kg/m³ for air
        val out = T.run(
            IdealGasCalculator,
            T.iv("p", 101.325, "kpa"), T.iv("t", 0.0, "c"), T.iv("m", 28.965, "gmol"),
        )
        assertEquals(1.2931, out.results.first { it.id == "rho" }.value, 0.005)
    }
}

class PowerTorqueRpmTest {

    @Test
    fun torqueFromPowerAndSpeed() {
        val out = T.run(PowerTorqueRpmCalculator, T.iv("p", 10.0, "kw"), T.iv("n", 1500.0, "rpm"))
        assertEquals(63.667, out.results.first { it.id == "t" }.value, 0.05)
    }

    @Test
    fun powerFromTorqueAndSpeed() {
        val out = T.run(PowerTorqueRpmCalculator, T.iv("t", 100.0, "nm"), T.iv("n", 1750.0, "rpm"))
        assertEquals(18.3246, out.results.first { it.id == "p" }.value, 0.02)
    }

    @Test
    fun speedFromPowerAndTorque() {
        val out = T.run(PowerTorqueRpmCalculator, T.iv("p", 10.0, "kw"), T.iv("t", 63.667, "nm"))
        assertEquals(1499.98, out.results.first { it.id == "n" }.value, 1.0)
    }

    @Test
    fun allThreeRejected() {
        try {
            T.run(
                PowerTorqueRpmCalculator,
                T.iv("p", 10.0, "kw"), T.iv("t", 60.0, "nm"), T.iv("n", 1500.0, "rpm"),
            )
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
    }

    @Test
    fun onlyOneRejected() {
        try {
            T.run(PowerTorqueRpmCalculator, T.iv("p", 10.0, "kw"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.isNotEmpty())
        }
    }

    @Test
    fun imperialTorqueCase() {
        // 50 HP at 1800 rpm: P=37284.99 W → T = 9550×37.285/1800 = 197.86 N·m
        val out = T.run(PowerTorqueRpmCalculator, T.iv("p", 50.0, "hp"), T.iv("n", 1800.0, "rpm"))
        assertEquals(197.86, out.results.first { it.id == "t" }.value, 0.3)
    }
}

class TorsionalStressTest {

    @Test
    fun stressFromTorqueAndDiameter() {
        val out = T.run(TorsionalStressCalculator, T.iv("t", 100.0, "nm"), T.iv("d", 20.0, "mm"))
        assertEquals(63.662, out.results.first { it.id == "tau" }.value, 0.1)
    }

    @Test
    fun minimumDiameterFromAllowable() {
        val out = T.run(TorsionalStressCalculator, T.iv("t", 100.0, "nm"), T.iv("taual", 40.0, "mpa"))
        assertEquals(23.35, out.results.first { it.id == "dmin" }.value, 0.1)
    }

    @Test
    fun utilizationWhenBothGiven() {
        val out = T.run(
            TorsionalStressCalculator,
            T.iv("t", 100.0, "nm"), T.iv("d", 20.0, "mm"), T.iv("taual", 40.0, "mpa"),
        )
        assertEquals(1.5916, out.results.first { it.id == "util" }.value, 0.01)
    }

    @Test
    fun neitherDiameterNorAllowableRejected() {
        try {
            T.run(TorsionalStressCalculator, T.iv("t", 100.0, "nm"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }
}
