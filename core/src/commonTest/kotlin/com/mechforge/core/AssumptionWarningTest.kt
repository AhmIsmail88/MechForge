package com.mechforge.core

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Engineering review P1-6: every implicit default must name itself.
 *
 * The engine appends one warning per input the caller omitted while the calculator falls
 * back to a built-in number, so an assumed value can never be applied silently. The second
 * test keeps the documentation in place: the registry must keep declaring its assumptions.
 */
class AssumptionWarningTest {

    private fun run(calcId: String, vararg inputs: Triple<String, Double, String>) =
        CalculatorRegistry.byIdOrThrow(calcId).run(
            inputs.associate { (id, value, unit) -> id to InputValue(id, value, unit) }
        )

    @Test
    fun anOmittedAssumedInputIsReported() {
        // CR_max left out: the calculator still runs, but it must say what it assumed
        val assumed = run("compression-ratio", Triple("p1", 4.0e5, "bar"), Triple("p2", 16.0e5, "bar"))
        val hits = assumed.warnings.filter { it.contains("Maximum per-stage ratio assumed as 4.0") }
        assertTrue(hits.size == 1, "expected exactly one CR_max caveat, got ${assumed.warnings}")
        assertTrue(assumed.warnings.first() == hits.first(), "the assumption must lead the warnings")

        // CR_max supplied: no assumption, no caveat
        val supplied = run(
            "compression-ratio",
            Triple("p1", 4.0e5, "bar"),
            Triple("p2", 16.0e5, "bar"),
            Triple("crmax", 2.5, "dash"),
        )
        assertTrue(
            supplied.warnings.none { it.contains("Maximum per-stage ratio assumed") },
            "a supplied CR_max must not raise the caveat: ${supplied.warnings}",
        )

        // an assumed default must not change the arithmetic, only announce itself
        val assumedCr = assumed.results.first { it.id == "cr" }.value
        val suppliedCr = supplied.results.first { it.id == "cr" }.value
        assertTrue(assumedCr == suppliedCr, "CR changed by the assumption warning path")
    }

    @Test
    fun theRegistryKeepsDeclaringItsAssumptions() {
        val annotated = CalculatorRegistry.all.flatMap { calculator ->
            calculator.def.inputs
                .filter { it.assumedWhenOmitted != null }
                .map { "${calculator.def.id}.${it.id}" }
        }
        assertTrue(
            annotated.size >= 20,
            "expected at least 20 documented assumptions across the registry, found " +
                "${annotated.size}: $annotated",
        )
        assertTrue(
            annotated.any { it == "compression-ratio.crmax" },
            "compression-ratio.crmax lost its caveat: $annotated",
        )
    }
}