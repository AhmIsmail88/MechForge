package com.mechforge.core

import com.mechforge.core.units.UnitFamily
import com.mechforge.core.units.Units
import com.mechforge.core.util.Fmt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UnitsTest {

    private fun rt(value: Double, unitId: String, relTol: Double = 1e-9): Boolean {
        val u = Units.byId(unitId)
        val base = u.toBase(value)
        val back = u.fromBase(base)
        return abs(back - value) <= relTol * maxOf(abs(value), 1e-30)
    }

    @Test
    fun temperatureOffsets() {
        assertEquals(273.15, Units.convert(0.0, "c", "k"), 1e-9)
        assertEquals(100.0, Units.convert(212.0, "f", "c"), 1e-9)
        assertEquals(32.0, Units.convert(0.0, "c", "f"), 1e-9)
        assertEquals(-40.0, Units.convert(-40.0, "f", "c"), 1e-9)
    }

    @Test
    fun pressureConversions() {
        assertEquals(1.0, Units.convert(1e5, "pa", "bar"), 1e-9)
        assertEquals(6.894757293, Units.convert(1.0, "psi", "kpa"), 1e-6)
        assertEquals(10.332275, Units.convert(101325.0, "pa", "mh2o"), 1e-4)
    }

    @Test
    fun flowConversions() {
        assertEquals(0.0630901964, Units.convert(1.0, "gpm", "ls"), 1e-9)
        assertEquals(0.58857778, Units.convert(1.0, "m3h", "cfm"), 1e-7)
        assertEquals(60000.0, Units.convert(1.0, "m3s", "lmin"), 1e-6)
    }

    @Test
    fun powerConversions() {
        assertEquals(3.5168528, Units.convert(1.0, "tr", "kw"), 1e-8)
        assertEquals(0.74569987158, Units.convert(1.0, "hp", "kw"), 1e-9)
        assertEquals(3412.142, Units.convert(1.0, "kw", "btuh"), 1e-3)
    }

    @Test
    fun familyMismatchThrows() {
        assertFailsWith<IllegalArgumentException> {
            Units.convert(1.0, "bar", "kw")
        }
    }

    @Test
    fun roundTripInvariantsAllUnits() {
        for (unit in Units.all) {
            val probe = if (unit.family == UnitFamily.TEMPERATURE) 25.0 else 1.0
            assertTrue(rt(probe, unit.id), "round-trip failed for ${unit.id}")
            assertTrue(rt(-7.25, unit.id), "round-trip (negative) failed for ${unit.id}")
        }
    }
}

class FmtTest {

    @Test
    fun basicFormatting() {
        assertEquals("13.62", Fmt.n(13.6204))
        assertEquals("13.620", Fmt.n(13.6204, 3))
        assertEquals("100.00", Fmt.n(100.0))
    }

    @Test
    fun roundingCarry() {
        assertEquals("1.00", Fmt.n(0.999, 2))
        assertEquals("0.00", Fmt.n(0.0001, 2))
        assertEquals("0.00", Fmt.n(-0.00001, 2))
    }

    @Test
    fun negativeValues() {
        assertEquals("-3.54", Fmt.n(-3.5367))
        assertEquals("0.00", Fmt.n(-0.0, 2))
    }
}
