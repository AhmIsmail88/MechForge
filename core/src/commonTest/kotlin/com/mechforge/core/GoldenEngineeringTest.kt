package com.mechforge.core

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Golden engineering test suite (audit P0-3).
 *
 * Test 1 runs the frozen scenario table in `GoldenCases.kt` through the engine and compares
 * every result against a value re-derived independently in Python from the textbook
 * equations - never against a previously recorded engine output, so a wrong result cannot
 * ratify itself.
 *
 * Test 2 covers the conversion engine itself (audit P0-1 / section 19): every declared unit
 * must round-trip through its base unit without loss, and the factors that the engineering
 * review questioned are pinned to their exact values.
 */
class GoldenEngineeringTest {

    @Test
    fun everyGoldenCaseMatchesTheIndependentModel() {
        val failures = mutableListOf<String>()
        val calculators = mutableSetOf<String>()
        var comparisons = 0

        for (case in GOLDEN_CASES) {
            val calculator = CalculatorRegistry.byIdOrThrow(case.calculatorId)
            val inputs = case.inputs.associate {
                it.inputId to InputValue(it.inputId, it.baseValue, it.displayUnitId)
            }
            val output = runCatching { calculator.run(inputs) }
                .getOrElse {
                    failures += "${case.calculatorId}/${case.scenario}: threw ${it.message}"
                    continue
                }
            calculators += case.calculatorId
            val actual = output.results.associate { it.id to it.value }

            for ((id, expectedValue) in case.expected) {
                val got = actual[id]
                if (got == null) {
                    failures += "${case.calculatorId}/${case.scenario}: result '$id' missing"
                    continue
                }
                comparisons++
                val tolerance = maxOf(1e-9, abs(expectedValue) * case.relativeTolerance)
                if (abs(got - expectedValue) > tolerance) {
                    failures += "${case.calculatorId}/${case.scenario}.$id: expected " +
                        "$expectedValue, engine $got, delta ${abs(got - expectedValue)}"
                }
            }
        }

        println(
            "golden: ${GOLDEN_CASES.size} cases, $comparisons comparisons, " +
                "${calculators.size} calculators"
        )
        if (failures.isNotEmpty()) {
            throw AssertionError(
                "golden mismatches (${failures.size}):\n" + failures.joinToString("\n")
            )
        }
        assertTrue(comparisons >= 400, "expected at least 400 golden comparisons, got $comparisons")
    }

    @Test
    fun unitConversionRoundTripsAndMatchesKnownValues() {
        val failures = mutableListOf<String>()

        // 1. every declared unit converts into its base unit and back without loss
        for (unit in Units.all) {
            for (value in listOf(1.0, 30.0, 1234.5)) {
                val back = unit.fromBase(unit.toBase(value))
                val tolerance = maxOf(1e-9, abs(value) * 1e-9)
                if (abs(back - value) > tolerance) {
                    failures += "${unit.id}: $value -> base -> $back"
                }
            }
        }

        // 2. factors questioned by the engineering review, pinned to their exact values
        val known = listOf(
            Triple("m3h", 100.0, 100.0 / 3600.0),            // 1 m3/h = 2.7777...e-4 m3/s
            Triple("m3d", 1.0, 1.0 / 86400.0),
            Triple("kgh", 3600.0, 1.0),
            Triple("kgd", 86400.0, 1.0),
            Triple("lbh", 3600.0, 0.45359237),
            Triple("kmh", 3.6, 1.0),
            Triple("fpm", 1.0, 0.3048 / 60.0),
            Triple("rads", 1.0, 60.0 / (2.0 * Math.PI)),     // 9.549296586, not 60
            Triple("delf", 9.0, 5.0),                        // 9 dF = 5 K, not 45
            Triple("inh2operft", 12.0, 9806.65),             // 12 inH2O/ft = 1 inH2O/in
            Triple("gpmpsi", 1.0, 14.41704),                 // 1 gpm/psi^0.5 in L/min/bar^0.5
        )
        for ((unitId, input, expected) in known) {
            val got = Units.byId(unitId).toBase(input)
            val tolerance = maxOf(1e-9, abs(expected) * 1e-9)
            if (abs(got - expected) > tolerance) {
                failures += "$unitId: $input -> expected $expected, got $got"
            }
        }

        if (failures.isNotEmpty()) {
            throw AssertionError(
                "unit conversion failures (${failures.size}):\n" + failures.joinToString("\n")
            )
        }
        println("units: ${Units.all.size} units round-tripped, ${known.size} known values matched")
    }
}