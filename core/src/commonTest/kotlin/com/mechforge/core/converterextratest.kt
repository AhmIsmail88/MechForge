package com.mechforge.core

import com.mechforge.core.calcs.FlowConverterCalculator
import com.mechforge.core.calcs.LengthConverterCalculator
import com.mechforge.core.calcs.PowerConverterCalculator
import com.mechforge.core.calcs.PressureConverterCalculator
import com.mechforge.core.calcs.TemperatureConverterCalculator
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private fun valueOf(calc: Calculator, resultId: String, vararg inputs: InputValue): Double =
    T.run(calc, *inputs).results.first { it.id == resultId }.value

class ConverterExtraTest {

    @Test
    fun pressureConverter() {
        val bar = T.iv("v", 1.0, "bar")
        assertEquals(100.0, valueOf(PressureConverterCalculator, "kpa", bar), 0.001)
        assertEquals(14.5038, valueOf(PressureConverterCalculator, "psi", bar), 0.001)
        assertEquals(10.1972, valueOf(PressureConverterCalculator, "mh2o", bar), 0.001)
        assertEquals(100000.0, valueOf(PressureConverterCalculator, "pa", bar), 0.1)
    }

    @Test
    fun flowConverter() {
        val hour = T.iv("v", 1.0, "m3h")
        assertEquals(0.588578, valueOf(FlowConverterCalculator, "cfm", hour), 1e-5)
        assertEquals(0.277778, valueOf(FlowConverterCalculator, "ls", hour), 1e-5)
        assertEquals(4.40287, valueOf(FlowConverterCalculator, "gpm", hour), 1e-4)
        assertEquals(1.0, valueOf(FlowConverterCalculator, "m3h", hour), 1e-9)
    }

    @Test
    fun powerConverter() {
        val kw = T.iv("v", 1.0, "kw")
        assertEquals(1.34102, valueOf(PowerConverterCalculator, "hp", kw), 1e-4)
        assertEquals(0.284345, valueOf(PowerConverterCalculator, "tr", kw), 1e-5)
        assertEquals(3412.142, valueOf(PowerConverterCalculator, "btuh", kw), 0.01)
    }

    @Test
    fun lengthConverter() {
        val metre = T.iv("v", 1.0, "m")
        assertEquals(3.28084, valueOf(LengthConverterCalculator, "ft", metre), 1e-4)
        assertEquals(39.3701, valueOf(LengthConverterCalculator, "in", metre), 1e-3)
        assertEquals(1000.0, valueOf(LengthConverterCalculator, "mm", metre), 1e-6)
    }

    @Test
    fun temperatureConverter() {
        val hundred = T.iv("v", 100.0, "c")
        assertEquals(212.0, valueOf(TemperatureConverterCalculator, "f", hundred), 1e-6)
        assertEquals(373.15, valueOf(TemperatureConverterCalculator, "k", hundred), 1e-6)
    }

    @Test
    fun familyMismatchRejected() {
        try {
            // a power unit is not valid for the pressure converter
            T.run(PressureConverterCalculator, InputValue("v", 1000.0, "kw"))
            fail("expected ValidationException")
        } catch (e: ValidationException) {
            assertTrue(e.errors.any { it.inputId == "v" })
        }
    }
}
