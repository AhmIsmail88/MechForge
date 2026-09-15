package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Water hammer pressure rise (Joukowsky) and critical closure time. */
private val Def = CalculatorDefinition(
    id = "water-hammer",
    name = "Water Hammer (Joukowsky)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Instantaneous pressure rise from a rapid velocity change in a pipeline, and the critical closure time for the pipe length.",
    formulaDisplay = "dP = rho * c * dv ;  t_critical = 2L/c",
    reference = "Joukowsky (1898) surge relation; standard water-hammer texts (e.g. Wylie & Streeter).",
    notes = "c is the pressure-wave speed (about 1000-1250 m/s in steel water pipes, lower in plastic). The Joukowsky value is the maximum (instantaneous closure); slower closure reduces the surge. Pipe length is optional and only used for the critical time.",
    keywords = listOf("water hammer", "surge", "joukowsky", "transient", "valve closure", "fire"),
    inputs = listOf(
        InputSpec("rho", "Fluid density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
        InputSpec("c", "Pressure wave speed", "c", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("dv", "Velocity change", "dv", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("l", "Pipe length (optional)", "L", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
    ),
)

object WaterHammerCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val rho = optionalValue(inputs, "rho", 1000.0)
        val c = value(inputs, "c")
        val dv = value(inputs, "dv")

        val dpPa = rho * c * dv
        val surgeHead = dpPa / (rho * 9.80665)

        val results = mutableListOf(
            result("dp", "Pressure Rise (Joukowsky)", dpPa / 1e5, "bar", isPrimary = true),
            result("dpMpa", "Pressure Rise (MPa)", dpPa / 1e6, "mpa"),
            result("head", "Surge Head", surgeHead, "m", isPrimary = true),
        )
        val steps = mutableListOf(
            "dP = rho * c * dv = ${Fmt.n(rho, 1)} x ${Fmt.n(c, 1)} x ${Fmt.n(dv, 3)} = ${Fmt.n(dpPa, 0)} Pa = ${Fmt.n(dpPa / 1e5, 3)} bar",
            "Surge head: dP/(rho*g) = ${Fmt.n(surgeHead, 1)} m",
        )

        if (has(inputs, "l")) {
            val l = value(inputs, "l")
            val tCritical = 2.0 * l / c
            results += result("tc", "Critical Closure Time", tCritical, "s")
            steps += "Critical closure time: t = 2L/c = 2 x ${Fmt.n(l, 1)} / ${Fmt.n(c, 1)} = ${Fmt.n(tCritical, 4)} s (closure faster than this gives the full Joukowsky rise)"
        }

        return CalcOutput(
            results = results,
            steps = steps,
            warnings = buildList {
                if (!has(inputs, "rho")) add("Density not provided - assumed 1000 kg/m3 (water).")
                add("This is the maximum (instantaneous closure) surge. For slow closures use the standard wave-speed characteristics or a surge-analysis package.")
                if (dv > 3.0) add("Velocity change above 3 m/s produces a very large surge - review valve closure time and pipe pressure class.")
            },
        )
    }
}
