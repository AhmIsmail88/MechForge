package com.mechforge.core

import com.mechforge.core.calcs.PipeVelocityCalculator
import com.mechforge.core.calcs.HazenWilliamsCalculator
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Shared helpers + spec/registry-level tests. */
object T {
    /** Build an InputValue from a value in its display unit (converted to base). */
    fun iv(id: String, value: Double, unitId: String): InputValue =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    fun run(calc: Calculator, vararg inputs: InputValue) =
        calc.run(inputs.associateBy { it.inputId })
}

class ValidationTest {

    @Test
    fun requiredMissing() {
        try {
            T.run(PipeVelocityCalculator, T.iv("q", 10.0, "m3h"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }

    @Test
    fun belowExclusiveMinimum() {
        try {
            T.run(PipeVelocityCalculator, T.iv("q", 10.0, "m3h"), T.iv("d", 0.0, "mm"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" })
        }
    }

    @Test
    fun unitFamilyMismatch() {
        try {
            T.run(PipeVelocityCalculator, T.iv("q", 10.0, "m3h"), T.iv("d", 100.0, "kw"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "d" && it.message.contains("Invalid unit") })
        }
    }

    @Test
    fun nonFiniteInputIsRejectedBeforeCalculation() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            try {
                T.run(PipeVelocityCalculator, T.iv("q", 10.0, "m3h"), T.iv("d", bad, "mm"))
                fail("expected ValidationException for $bad")
            } catch (e: ValidationException) {
                assertTrue(e.errors.any { it.inputId == "d" && it.message.contains("finite") })
            }
        }
    }

    @Test
    fun hazenWilliamsGradientUsesActualLength() {
        val out = T.run(
            HazenWilliamsCalculator,
            T.iv("c", 130.0, "dash"), T.iv("d", 300.0, "mm"),
            T.iv("q", 100.0, "m3h"), T.iv("l", 250.0, "m"),
        )
        val headLoss = out.results.first { it.id == "hf" }.value
        val gradient = out.results.first { it.id == "gradient" }.value
        assertEquals(headLoss / 250.0, gradient, 1e-12)
        assertTrue(out.steps.any { it.contains("L = 250") })
    }
}

class RegistryTest {

    @Test
    fun calculatorCountMatchesMvpTarget() {
        assertEquals(64, CalculatorRegistry.all.size)
    }

    @Test
    fun idsAreUnique() {
        val ids = CalculatorRegistry.all.map { it.def.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun categoryCounts() {
        assertEquals(10, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.HYDRAULICS).size)
        assertEquals(11, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.HVAC).size)
        assertEquals(6, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.THERMODYNAMICS).size)
        assertEquals(8, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.MECHANICAL_DESIGN).size)
        assertEquals(7, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.PIPING).size)
        assertEquals(5, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.WATER_WASTEWATER).size)
        assertEquals(7, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.FIRE_PROTECTION).size)
        assertEquals(5, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.EQUIPMENT).size)
        assertEquals(5, CalculatorRegistry.byCategory.getValue(com.mechforge.core.engine.CalculatorCategory.UNIT_CONVERSION).size)
    }

    @Test
    fun everyCalculatorHasMetadata() {
        for (calc in CalculatorRegistry.all) {
            val d = calc.def
            assertTrue(d.name.isNotBlank(), "${d.id} has no name")
            assertTrue(d.formulaDisplay.isNotBlank(), "${d.id} has no formula")
            assertTrue(d.reference.isNotBlank(), "${d.id} has no reference")
            assertTrue(d.inputs.isNotEmpty(), "${d.id} has no inputs")
        }
    }

    @Test
    fun searchFindsByNameAndKeywordAndCategory() {
        assertTrue(CalculatorRegistry.search("pump").any { it.def.id == "pump-power" })
        assertTrue(CalculatorRegistry.search("reynolds").any { it.def.id == "reynolds-number" })
        assertTrue(CalculatorRegistry.search("hydraulics").size >= 4)
        assertTrue(CalculatorRegistry.search("fm-200").any { it.def.id == "fm200-agent-quantity" })
        assertTrue(CalculatorRegistry.search("co2").any { it.def.id == "co2-agent-quantity" })
        assertTrue(CalculatorRegistry.search("zzz-no-match").isEmpty())
    }
}
