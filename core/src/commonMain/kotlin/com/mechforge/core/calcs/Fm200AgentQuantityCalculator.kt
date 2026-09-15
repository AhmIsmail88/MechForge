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
import kotlin.math.ceil

/**
 * HFC-227ea (FM-200) total flooding agent quantity.
 *
 * Workflow (NFPA 2001 style): pick the hazard class -> the design concentration follows ->
 *
 *     W = (V / S) * (C / (100 - C))
 *
 * V = protected enclosure volume (m3), C = design concentration (% by volume),
 * S = specific vapour volume of the agent at the design temperature (m3/kg).
 *
 * S comes from the agent's vapour density, S = R*T/(P*M) with M = 170.03 g/mol for
 * HFC-227ea, and can be overridden with the value from the project's design data.
 * The class concentrations below are the commonly used design values for HFC-227ea and
 * are editable: NFPA 2001 sets the minimum design concentration from the fuel data, and
 * the manufacturer's design manual is the authority for a specific hazard.
 */
private const val R_GAS = 8.31446261815324 // J/(mol*K)
private const val P_STD = 101325.0 // Pa
private const val M_HFC_227EA = 0.17003 // kg/mol

/** Hazard class -> typical design concentration (% by volume) for HFC-227ea. */
private val HazardClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("a", "Class A - solid combustibles, surface fire") to 7.0,
    InputOption("b", "Class B - flammable liquids") to 8.7,
    InputOption("c", "Class C - electrical / electronic equipment") to 6.25,
)

private val Def = CalculatorDefinition(
    id = "fm200-agent-quantity",
    name = "FM-200 (HFC-227ea) Total Flooding Quantity",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "FM-200 clean agent for a total flooding system: choose the hazard class and enter the room volume - the design concentration, agent quantity and cylinder count follow.",
    formulaDisplay = "W = (V / S) * (C / (100 - C))   with   S = R * T / (P * M)",
    reference = "NFPA 2001 total flooding equation W = (V/S)*(C/(100-C)); design concentration from the hazard class (typical HFC-227ea values).",
    notes = "Pick the hazard class to set the design concentration, or enter your own value in the override field. S is the specific vapour volume of the agent at the design temperature (m3/kg); it is derived here from the ideal-gas vapour density and can be overridden with the project's value. W is the quantity inside the protected volume - add the manufacturer's allowance for piping/nozzle content, enclosure leakage and the design margin. Minimum design concentrations are code and manufacturer data: confirm them with NFPA 2001 and the agent manufacturer's design manual.",
    keywords = listOf("fm-200", "fm200", "hfc-227ea", "hfc 227ea", "clean agent", "total flooding", "nfpa 2001", "fire", "suppression", "gaseous", "hazard class", "class a", "class b", "class c"),
    inputs = listOf(
        InputSpec("v", "Protected enclosure volume", "V", UnitFamily.VOLUME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec(
            "hazard", "Hazard class (sets the design concentration)", "Hazard", UnitFamily.DIMENSIONLESS,
            allowedUnitIds = listOf("dash"), defaultUnitId = "dash", defaultValue = 0.0,
            options = HazardClasses.map { it.first },
        ),
        InputSpec(
            "c", "Design concentration (override)", "C", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, exclusiveMax = true,
            allowedUnitIds = listOf("pct"), defaultUnitId = "pct",
        ),
        InputSpec("t", "Design temperature", "T", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c", defaultValue = 21.0),
        InputSpec(
            "s", "Specific vapour volume (override)", "S", UnitFamily.SPECIFIC_VOLUME,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3perkg",
        ),
        InputSpec(
            "mcyl", "Nominal agent charge per cylinder", "mcyl", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kg",
        ),
    ),
)

object Fm200AgentQuantityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val volume = value(inputs, "v") // m3
        val hazard = HazardClasses[value(inputs, "hazard").toInt()]
        val concentrationPct = if (has(inputs, "c")) value(inputs, "c") * 100.0 else hazard.second
        val cFraction = concentrationPct / 100.0
        val temperatureK = value(inputs, "t") // K
        val sDerived = !has(inputs, "s")
        val s = if (sDerived) R_GAS * temperatureK / (P_STD * M_HFC_227EA) else value(inputs, "s")

        val ratio = cFraction / (1.0 - cFraction)
        val mass = (volume / s) * ratio // kg
        val massLb = mass / 0.45359237
        val floodingFactor = mass / volume // kg/m3
        val vapourVolume = mass * s // m3 of agent vapour at the design temperature

        val warnings = buildList {
            add("The class concentration is a typical design value: confirm the minimum design concentration for the actual fuel and hazard with NFPA 2001 and the agent manufacturer's design manual.")
            add("W is the quantity inside the protected volume only. Add the manufacturer's allowance for piping and nozzle content, enclosure leakage and the required design margin.")
        }

        val results = buildList {
            add(result("w", "Agent quantity (kg)", mass, "kg", isPrimary = true))
            add(result("wLb", "Agent quantity (imperial)", massLb, "lb", isPrimary = true))
            add(result("cUsed", "Design concentration used", concentrationPct, "pct"))
            add(result("f", "Flooding factor", floodingFactor, "kgm3"))
            add(result("fLb", "Flooding factor (imperial)", floodingFactor / 16.0184634, "lbft3"))
            add(result("vapourVolume", "Agent vapour volume at design T", vapourVolume, "m3"))
            add(result("sUsed", "Specific vapour volume used", s, "m3perkg"))
            if (has(inputs, "mcyl")) {
                val charge = value(inputs, "mcyl")
                add(result("cylinders", "Cylinders required (${Fmt.n(charge, 3)} kg each)", ceil(mass / charge), "dash"))
            }
        }

        return CalcOutput(
            results = results,
            steps = listOf(
                "Hazard class: ${hazard.first.label} -> C = ${Fmt.n(concentrationPct, 4)} %" +
                    if (has(inputs, "c")) " (override entered)" else "",
                "V = ${Fmt.n(volume, 4)} m3   T = ${Fmt.n(temperatureK, 4)} K",
                if (sDerived) {
                    "S = R * T / (P * M) = ${Fmt.n(R_GAS, 5)} x ${Fmt.n(temperatureK, 4)} / (${Fmt.n(P_STD, 6)} x ${Fmt.n(M_HFC_227EA, 5)}) = ${Fmt.n(s, 6)} m3/kg"
                } else {
                    "S = ${Fmt.n(s, 6)} m3/kg (entered)"
                },
                "C / (100 - C) = ${Fmt.n(concentrationPct, 4)} / ${Fmt.n(100.0 - concentrationPct, 4)} = ${Fmt.n(ratio, 6)}",
                "W = (V / S) * (C / (100 - C)) = (${Fmt.n(volume, 4)} / ${Fmt.n(s, 6)}) x ${Fmt.n(ratio, 6)} = ${Fmt.n(mass, 3)} kg",
                "Agent vapour volume = W * S = ${Fmt.n(vapourVolume, 4)} m3 (equals V * C / (100 - C))",
            ),
            warnings = warnings,
        )
    }
}
