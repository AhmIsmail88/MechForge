package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.sqrt

/**
 * Joukowsky surge pressure, with the pressure-wave speed either entered directly or
 * calculated from the pipe and fluid properties (Korteweg / thin-walled elastic pipe):
 *
 *     c = sqrt(K / rho) / sqrt(1 + K * D / (E * t))
 *     dP = rho * c * dv
 *     t_critical = 2 * L / c
 */
private val Def = CalculatorDefinition(
    id = "water-hammer",
    name = "Water Hammer (Joukowsky)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Maximum surge pressure from a sudden velocity change, with the pressure-wave speed either entered or calculated from the fluid bulk modulus and the pipe properties.",
    formulaDisplay = "dP = rho*c*dv ;  c = sqrt(K/rho) / sqrt(1 + K*D/(E*t)) ;  t_critical = 2L/c",
    reference = "Joukowsky (1898) surge relation with the classical wave-speed formula for a thin-walled elastic pipe (Korteweg; Wylie & Streeter, Fluid Transients in Systems).",
    notes = "The Joukowsky value is the maximum surge, reached when the valve closes faster than the critical time 2L/c; slower closure reduces it. Leave the wave speed empty to calculate it from the fluid bulk modulus K (2.15 GPa for water), the pipe elastic modulus E (200 GPa for steel) and the pipe internal diameter and wall thickness. Typical wave speeds: about 1000-1250 m/s in steel water pipes and 300-500 m/s in plastic pipe.",
    keywords = listOf("water hammer", "surge", "joukowsky", "wave speed", "transient", "pipe", "valve closure", "fire"),
    inputs = listOf(
        InputSpec("rho", "Fluid density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
        InputSpec("c", "Pressure wave speed (leave empty to calculate)", "c", UnitFamily.VELOCITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("dv", "Velocity change", "dv", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("d", "Pipe internal diameter", "D", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("t", "Pipe wall thickness", "t", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("kbulk", "Fluid bulk modulus K", "K", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa", defaultValue = 2150.0),
        InputSpec("epipe", "Pipe elastic modulus E", "E", UnitFamily.PRESSURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa", defaultValue = 200000.0),
        InputSpec("l", "Pipe length (optional)", "L", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
    ),
)

object WaterHammerCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val rho = optionalValue(inputs, "rho", 1000.0)
        val dv = value(inputs, "dv")
        val cEntered = has(inputs, "c")
        val cPipeData = has(inputs, "d") && has(inputs, "t")

        val c: Double = when {
            cEntered -> value(inputs, "c")
            cPipeData -> {
                val k = optionalValue(inputs, "kbulk", 2.15e9)
                val ePipe = optionalValue(inputs, "epipe", 200e9)
                sqrt(k / rho) / sqrt(1.0 + k * value(inputs, "d") / (ePipe * value(inputs, "t")))
            }
            else -> throw ValidationException(
                listOf(
                    InputError(
                        "c",
                        "Enter the pressure wave speed, or the pipe internal diameter and wall thickness so it can be calculated.",
                    ),
                ),
            )
        }

        val dpPa = rho * c * dv
        val surgeHead = dpPa / (rho * 9.80665)

        val results = mutableListOf(
            result("dp", "Pressure Rise (Joukowsky)", dpPa / 1e5, "bar", isPrimary = true),
            result("dpMpa", "Pressure Rise (MPa)", dpPa / 1e6, "mpa"),
            result("head", "Surge Head", surgeHead, "m", isPrimary = true),
            result("cUsed", "Pressure Wave Speed Used", c, "ms"),
        )
        val steps = mutableListOf(
            if (cEntered) {
                "Wave speed: c = ${Fmt.n(c, 1)} m/s (entered)"
            } else {
                "Wave speed: c = sqrt(K/rho) / sqrt(1 + K*D/(E*t)) = ${Fmt.n(c, 1)} m/s (calculated from the pipe data)"
            },
            "dP = rho*c*dv = ${Fmt.n(rho, 1)} x ${Fmt.n(c, 1)} x ${Fmt.n(dv, 3)} = ${Fmt.n(dpPa, 0)} Pa = ${Fmt.n(dpPa / 1e5, 3)} bar",
            "Surge head: dP/(rho*g) = ${Fmt.n(surgeHead, 1)} m",
        )

        if (has(inputs, "l")) {
            val l = value(inputs, "l")
            val tCritical = 2.0 * l / c
            results += result("tc", "Critical Closure Time", tCritical, "s")
            steps += "Critical closure time: t = 2L/c = 2 x ${Fmt.n(l, 1)} / ${Fmt.n(c, 1)} = ${Fmt.n(tCritical, 4)} s (closure faster than this gives the full Joukowsky rise)"
        }

        val warnings = buildList {
            if (!has(inputs, "rho")) add("Density not provided - assumed 1000 kg/m3 (water).")
            if (!cEntered && !has(inputs, "kbulk")) add("Fluid bulk modulus not provided - assumed 2.15 GPa (water at ambient temperature).")
            if (!cEntered && !has(inputs, "epipe")) add("Pipe elastic modulus not provided - assumed 200 GPa (steel). Use the value for the actual pipe material (plastic pipes give much lower wave speeds).")
            add("This is the maximum (instantaneous closure) surge. For slower closures use the standard wave-speed characteristics or a surge-analysis package.")
            if (dv > 3.0) add("Velocity change above 3 m/s produces a very large surge - review the valve closure time and the pipe pressure class.")
        }

        return CalcOutput(results = results, steps = steps, warnings = warnings)
    }
}
