package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputOption
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.pow

/**
 * Rolling bearing rating life.
 *
 *     L10   = (C/P)^p * 10^6 revolutions          (basic rating life, 90 % reliability)
 *     L_nm  = a1 * a_ISO * L10                    (modified rating life, ISO 281)
 *
 * The a1 factors for reliability follow ISO 281; a_ISO comes from the lubrication and
 * contamination of the application (viscosity ratio and contamination factor) and is
 * entered from the lubricant data or the bearing manufacturer's calculation.
 */
private val ReliabilityLevels: List<Pair<InputOption, Double>> = listOf(
    InputOption("90", "90 % - basic rating life (L10)") to 1.0,
    InputOption("95", "95 % (L5)") to 0.62,
    InputOption("96", "96 % (L4)") to 0.53,
    InputOption("97", "97 % (L3)") to 0.44,
    InputOption("98", "98 % (L2)") to 0.33,
    InputOption("99", "99 % (L1)") to 0.21,
)

private val Def = CalculatorDefinition(
    id = "bearing-l10",
    name = "Bearing Life (L10 / Lnm)",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Basic rating life of a rolling bearing (ISO 281 L10) and the reliability- and lubrication-adjusted modified life Lnm.",
    formulaDisplay = "L10 = (C/P)^p * 10^6 rev ;  L10h = 10^6/(60*n)*(C/P)^p ;  L_nm = a1 * a_ISO * L10",
    reference = "ISO 281 basic rating life L10 = (C/P)^p*10^6 with a1 reliability factors and the a_ISO life modification factor (ISO 281:2007).",
    notes = "p = 3 for ball bearings and 10/3 for roller bearings. The reliability factor a1 comes from ISO 281 (1.0 at 90 %, 0.62 at 95 %, 0.53 at 96 %, 0.44 at 97 %, 0.33 at 98 %, 0.21 at 99 %). a_ISO (a_SKF for SKF bearings) is taken from the lubrication and contamination of the application - the viscosity ratio and the contamination factor - and is entered here from the lubricant data or the manufacturer's tool. Verify the actual life with the bearing manufacturer for the application.",
    keywords = listOf("bearing", "l10", "lnm", "life", "iso 281", "dynamic load", "ball", "roller", "reliability", "lubrication"),
    inputs = listOf(
        InputSpec("c", "Dynamic load rating", "C", UnitFamily.FORCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("p", "Equivalent dynamic load", "P", UnitFamily.FORCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("exp", "Life exponent p", "p", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash", assumedWhenOmitted = "Life exponent assumed as 3.0 (ball bearings) - use 10/3 for roller bearings."),
        InputSpec("n", "Rotational speed", "n", UnitFamily.ROTATIONAL_SPEED, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "rpm"),
        InputSpec(
            "rel", "Reliability (a1 factor)", "reliability", UnitFamily.DIMENSIONLESS,
            required = false, allowedUnitIds = listOf("dash"), defaultUnitId = "dash",
            options = ReliabilityLevels.map { it.first },
        ),
        InputSpec(
            "aiso", "Life modification factor a_ISO", "a_ISO", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash", defaultValue = 1.0,
        assumedWhenOmitted = "a_ISO assumed as 1.0 (normal lubrication and contamination) - use the ISO 281 life modification factor where the application demands it."),
    ),
)

object BearingL10Calculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val c = value(inputs, "c")
        val p = value(inputs, "p")
        val exponent = optionalValue(inputs, "exp", 3.0)
        val reliability = if (has(inputs, "rel")) ReliabilityLevels[value(inputs, "rel").toInt()] else ReliabilityLevels[0]
        val a1 = reliability.second
        val aIso = optionalValue(inputs, "aiso", 1.0)

        val ratio = (c / p).pow(exponent)
        val l10Revs = ratio * 1e6 // revolutions
        val lnmRevs = a1 * aIso * l10Revs

        val results = mutableListOf(
            result("l10", "Basic Rating Life L10 (revolutions)", l10Revs, "rev", isPrimary = a1 == 1.0 && aIso == 1.0),
        )
        val steps = mutableListOf(
            "Load ratio: C/P = ${Fmt.n(c / 1000.0, 3)} kN / ${Fmt.n(p / 1000.0, 3)} kN = ${Fmt.n(c / p, 3)}",
            "L10 = (C/P)^p * 10^6 = ${Fmt.n(c / p, 3)}^${Fmt.n(exponent, 2)} x 10^6 = ${Fmt.n(l10Revs / 1e6, 1)} x 10^6 revolutions",
        )

        if (has(inputs, "n")) {
            val n = value(inputs, "n")
            val hours = l10Revs / (60.0 * n)
            results += result("l10h", "Basic Life in Hours (L10h)", hours, "h", isPrimary = a1 == 1.0 && aIso == 1.0)
            steps += "L10h = L10/(60*n) = ${Fmt.n(l10Revs / 1e6, 1)}x10^6 / (60 x ${Fmt.n(n, 0)}) = ${Fmt.n(hours, 0)} h"
        }

        if (a1 != 1.0 || aIso != 1.0) {
            results += result("lnm", "Modified Rating Life Lnm (revolutions)", lnmRevs, "rev", isPrimary = true)
            results += result("a1", "Reliability factor a1", a1, "dash")
            results += result("aisoUsed", "Life modification factor a_ISO", aIso, "dash")
            steps += "Reliability: ${reliability.first.label} -> a1 = ${Fmt.n(a1, 2)}"
            steps += "Lnm = a1 * a_ISO * L10 = ${Fmt.n(a1, 2)} x ${Fmt.n(aIso, 3)} x ${Fmt.n(l10Revs / 1e6, 1)}x10^6 = ${Fmt.n(lnmRevs / 1e6, 1)} x 10^6 revolutions"
            if (has(inputs, "n")) {
                val n = value(inputs, "n")
                results += result("lnmh", "Modified Life in Hours (Lnmh)", lnmRevs / (60.0 * n), "h", isPrimary = true)
                steps += "Lnmh = Lnm/(60*n) = ${Fmt.n(lnmRevs / (60.0 * n), 0)} h"
            }
        }

        val warnings = buildList {
            if (!has(inputs, "n")) add("Speed not provided - hours-based life not computed.")
            if (!has(inputs, "rel")) add("Reliability not provided - basic rating life (90 %) reported.")
            if (a1 < 1.0) add("A reliability above 90 % shortens the rating life: a1 = ${Fmt.n(a1, 2)} from ISO 281.")
            add("a_ISO depends on the lubrication (viscosity ratio) and the contamination of the application; enter it from the lubricant data or the bearing manufacturer's calculation.")
        }

        return CalcOutput(results = results, steps = steps, warnings = warnings)
    }
}
