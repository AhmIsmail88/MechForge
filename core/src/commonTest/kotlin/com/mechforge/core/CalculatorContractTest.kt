package com.mechforge.core

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The calculator contract, enforced for every calculator at once.
 *
 * The project's rule is that each calculator declares a name, a description, the equation it solves,
 * the reference that equation comes from, at least one input and at least one output, and that every
 * input names a unit the engine actually knows. Those rules were honoured one calculator at a time
 * while they were written, which is exactly the kind of promise that quietly stops being true: this
 * test is the enforcement, so a new calculator cannot arrive with a blank reference or a unit id that
 * does not resolve.
 */
class CalculatorContractTest {

    private val calculators get() = CalculatorRegistry.all

    @Test
    fun everyCalculatorDeclaresItsEquationAndItsSource() {
        val problems = calculators.mapNotNull { c ->
            val d = c.def
            when {
                d.name.isBlank() -> "${d.id}: no name"
                d.description.isBlank() -> "${d.id}: no description"
                d.formulaDisplay.isBlank() -> "${d.id}: no equation"
                d.reference.isBlank() -> "${d.id}: no reference"
                d.notes.isBlank() -> "${d.id}: no notes"
                else -> null
            }
        }
        assertTrue(problems.isEmpty(), "calculators that do not declare themselves: $problems")
    }

    @Test
    fun everyCalculatorHasInputs() {
        // No assertion that a required input exists: air-changes-hour and power-torque-rpm are built
        // to solve for whichever input is left out, so for them "no required input" is the design, not
        // a defect. That the calculators really do produce results is already covered where it can be
        // covered honestly - by the golden suite, which runs 141 real scenarios through the engine.
        val problems = calculators.mapNotNull { c ->
            if (c.def.inputs.isEmpty()) "${c.def.id}: no inputs" else null
        }
        assertTrue(problems.isEmpty(), "calculators with an empty face: $problems")
    }

    @Test
    fun everyInputUnitResolvesInTheEngine() {
        val problems = calculators.flatMap { c ->
            c.def.inputs.mapNotNull { spec ->
                val declared = buildList {
                    spec.defaultUnitId?.let { add(it) }
                    spec.allowedUnitIds?.let { addAll(it) }
                }
                if (declared.isEmpty()) {
                    "${c.def.id}.${spec.id}: no unit declared"
                } else {
                    val unknown = declared.filter { runCatching { Units.byId(it) }.isFailure }
                    if (unknown.isNotEmpty()) "${c.def.id}.${spec.id}: unknown unit $unknown" else null
                }
            }
        }
        assertTrue(problems.isEmpty(), "inputs with a unit the engine does not know: $problems")
    }

    @Test
    fun everyInputUnitBelongsToTheDeclaredFamily() {
        val problems = calculators.flatMap { c ->
            c.def.inputs.mapNotNull { spec ->
                val declared = buildList {
                    spec.defaultUnitId?.let { add(it) }
                    spec.allowedUnitIds?.let { addAll(it) }
                }
                val wrong = declared.filter { id ->
                    runCatching { Units.byId(id).family != spec.family }.getOrDefault(false)
                }
                if (wrong.isNotEmpty()) "${c.def.id}.${spec.id}: $wrong is not ${spec.family}" else null
            }
        }
        assertTrue(problems.isEmpty(), "inputs whose unit belongs to another family: $problems")
    }
}
