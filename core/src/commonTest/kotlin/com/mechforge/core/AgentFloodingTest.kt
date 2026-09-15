package com.mechforge.core

import com.mechforge.core.calcs.Co2AgentQuantityCalculator
import com.mechforge.core.calcs.Fm200AgentQuantityCalculator
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Clean agent / inert gas total flooding quantities, hazard-class driven.
 *
 * Expected values were computed independently (python, ideal gas):
 *   FM-200 (M = 170.03 g/mol), V = 100 m3, T = 21 C -> S = 0.141958 m3/kg
 *     Class A (7.0 %) : W = 53.022 kg
 *     Class B (8.7 %) : W = 67.126 kg
 *     Class C (6.25%) : W = 46.962 kg
 *     override 7.5 %  : W = 57.116 kg
 *   CO2 (M = 44.01 g/mol), V = 100 m3, T = 21 C -> rho = 1.823329 kg/m3
 *     surface fires (34 %)  : f = 0.939290 kg/m3 -> W = 93.929 kg  (3 x 45 kg cylinders)
 *     deep-seated  (50 %)   : f = 1.823329 kg/m3 -> W = 182.333 kg (5 x 45 kg cylinders)
 */
class Fm200AgentQuantityTest {

    @Test
    fun hazardClassSetsTheDesignConcentration() {
        val classA = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertEquals(7.0, classA.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(0.141958, classA.results.first { it.id == "sUsed" }.value, 1e-5)
        assertEquals(53.0218, classA.results.first { it.id == "w" }.value, 0.01)
        assertEquals(0.530218, classA.results.first { it.id == "f" }.value, 1e-5)
        assertEquals(7.5269, classA.results.first { it.id == "vapourVolume" }.value, 0.005)

        val classB = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 1.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertEquals(8.7, classB.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(67.1255, classB.results.first { it.id == "w" }.value, 0.01)

        val classC = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 2.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertEquals(6.25, classC.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(46.9621, classC.results.first { it.id == "w" }.value, 0.01)
    }

    @Test
    fun concentrationOverrideBeatsTheHazardClass() {
        val out = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 7.5, "pct"), T.iv("t", 21.0, "c"),
        )
        assertEquals(7.5, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(57.1161, out.results.first { it.id == "w" }.value, 0.01)
    }

    @Test
    fun volumeUnitsAgreeAndImperialMassIsCorrect() {
        val metric = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        val litres = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100_000.0, "liter"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        val cubicFeet = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 3531.4667, "ft3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        val kg = metric.results.first { it.id == "w" }.value
        assertEquals(kg, litres.results.first { it.id == "w" }.value, 1e-9)
        assertEquals(kg, cubicFeet.results.first { it.id == "w" }.value, 0.01)
        assertEquals(116.89, metric.results.first { it.id == "wLb" }.value, 0.05)
    }

    @Test
    fun celsiusAndKelvinAgreeAndColdRoomNeedsMoreAgent() {
        val warm = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val kelvin = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 294.15, "k"),
        ).results.first { it.id == "w" }.value
        assertEquals(warm, kelvin, 1e-9)
        val cold = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 0.0, "c"),
        ).results.first { it.id == "w" }.value
        assertEquals(57.0981, cold, 0.01)
        assertTrue(cold > warm)
    }

    @Test
    fun specificVolumeOverrideReproducesTheDerivedValue() {
        val derived = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 7.5, "pct"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val overridden = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 7.5, "pct"), T.iv("t", 21.0, "c"),
            T.iv("s", 0.141958, "m3perkg"),
        ).results.first { it.id == "w" }.value
        assertEquals(derived, overridden, 0.05)
    }

    @Test
    fun cylinderCountIsOptionalAndRoundedUp() {
        val withoutCharge = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertTrue(withoutCharge.results.none { it.id == "cylinders" })
        val withCharge = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
            T.iv("mcyl", 40.0, "kg"),
        )
        // 53.022 / 40 = 1.33 -> 2 cylinders
        assertEquals(2.0, withCharge.results.first { it.id == "cylinders" }.value, 1e-9)
    }

    @Test
    fun badHazardIndexAndBadConcentrationAreRejected() {
        try {
            T.run(
                Fm200AgentQuantityCalculator,
                T.iv("v", 100.0, "m3"), T.iv("hazard", 5.0, "dash"), T.iv("t", 21.0, "c"),
            )
            fail("expected ValidationException for an out-of-range hazard index")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "hazard" })
        }
        try {
            T.run(
                Fm200AgentQuantityCalculator,
                T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 100.0, "pct"), T.iv("t", 21.0, "c"),
            )
            fail("expected ValidationException for C = 100 %")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "c" })
        }
    }

    @Test
    fun doublingTheRoomDoublesTheAgent() {
        val single = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val double = T.run(
            Fm200AgentQuantityCalculator,
            T.iv("v", 200.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        assertEquals(2.0, double / single, 1e-9)
        assertEquals(106.0435, double, 0.01)
    }
}

class Co2AgentQuantityTest {

    @Test
    fun surfaceFireHazardGivesThirtyFourPercentAndCylinderCount() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertEquals(34.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(1.823329, out.results.first { it.id == "rhoVapour" }.value, 1e-5)
        assertEquals(0.93929, out.results.first { it.id == "f" }.value, 1e-4)
        assertEquals(93.929, out.results.first { it.id == "w" }.value, 0.01)
        assertEquals(0.058638, out.results.first { it.id == "fLb" }.value, 1e-5)
        assertEquals(207.08, out.results.first { it.id == "wLb" }.value, 0.1)
        // standard 45 kg cylinder: 93.929 / 45 = 2.09 -> 3 cylinders
        assertEquals(3.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
    }

    @Test
    fun deepSeatedHazardGivesFiftyPercent() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 3.0, "dash"), T.iv("t", 21.0, "c"),
        )
        assertEquals(50.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(1.823329, out.results.first { it.id == "f" }.value, 1e-5)
        assertEquals(182.3329, out.results.first { it.id == "w" }.value, 0.01)
        assertEquals(5.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
    }

    @Test
    fun concentrationAndCylinderChargeCanBeOverridden() {
        val out = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 30.0, "pct"), T.iv("t", 21.0, "c"),
            T.iv("mcyl", 20.0, "kg"),
        )
        assertEquals(30.0, out.results.first { it.id == "cUsed" }.value, 1e-9)
        assertEquals(78.1427, out.results.first { it.id == "w" }.value, 0.01)
        // 78.1427 / 20 = 3.91 -> 4 cylinders
        assertEquals(4.0, out.results.first { it.id == "cylinders" }.value, 1e-9)
    }

    @Test
    fun volumeUnitsAgreeInImperialAndMetric() {
        val metric = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val cubicFeet = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 3531.4667, "ft3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        assertEquals(metric, cubicFeet, 0.01)
    }

    @Test
    fun colderEnclosureNeedsMoreCo2() {
        val warm = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val cold = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 0.0, "c"),
        ).results.first { it.id == "w" }.value
        assertEquals(101.1504, cold, 0.01)
        assertTrue(cold > warm)
    }

    @Test
    fun volumeScalesLinearly() {
        val one = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        val twoAndAHalf = T.run(
            Co2AgentQuantityCalculator,
            T.iv("v", 250.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", 21.0, "c"),
        ).results.first { it.id == "w" }.value
        assertEquals(2.5, twoAndAHalf / one, 1e-9)
        assertEquals(234.8226, twoAndAHalf, 0.01)
    }

    @Test
    fun badHazardIndexConcentrationAndTemperatureAreRejected() {
        try {
            T.run(
                Co2AgentQuantityCalculator,
                T.iv("v", 100.0, "m3"), T.iv("hazard", 9.0, "dash"), T.iv("t", 21.0, "c"),
            )
            fail("expected ValidationException for an out-of-range hazard index")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "hazard" })
        }
        try {
            T.run(
                Co2AgentQuantityCalculator,
                T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("c", 0.0, "pct"), T.iv("t", 21.0, "c"),
            )
            fail("expected ValidationException for C = 0 %")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "c" })
        }
        try {
            T.run(
                Co2AgentQuantityCalculator,
                T.iv("v", 100.0, "m3"), T.iv("hazard", 0.0, "dash"), T.iv("t", -300.0, "c"),
            )
            fail("expected ValidationException for a temperature below absolute zero")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "t" })
        }
    }
}
