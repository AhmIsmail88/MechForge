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
 * Carbon dioxide (CO2) total flooding agent quantity.
 *
 * Workflow (NFPA 12 style): pick the hazard class -> the design concentration follows ->
 *
 *     f = rho_vapour(T) * C / (100 - C)      (kg of CO2 per m3 of enclosure)
 *     W = V * f                              (kg of CO2)
 *     cylinders = ceil(W / charge)           (45 kg per cylinder is the common standard charge)
 *
 * rho_vapour is the CO2 vapour density at the design temperature (ideal gas, M = 44.01 g/mol).
 * The flooding factor is derived from that relation, so no flooding-factor table is embedded.
 */
private const val R_GAS = 8.31446261815324 // J/(mol*K)
private const val P_STD = 101325.0 // Pa
private const val M_CO2 = 0.04401 // kg/mol
private const val STANDARD_CYLINDER_CHARGE = 45.0 // kg

/** Hazard -> design concentration (% by volume) for CO2 total flooding. */
private val HazardClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("a", "Class A - surface fire, solid combustibles") to 34.0,
    InputOption("b", "Class B - flammable liquids") to 34.0,
    InputOption("c", "Class C - electrical / electronic equipment") to 34.0,
    InputOption("ds", "Class A - deep-seated (smouldering) fire") to 50.0,
)

private val Def = CalculatorDefinition(
    id = "co2-agent-quantity",
    name = "CO2 Total Flooding Quantity (NFPA 12)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "CO2 total flooding system: choose the hazard class and enter the room volume - the design concentration, flooding factor, CO2 quantity and number of 45 kg cylinders follow.",
    formulaDisplay = "f = rho_vapour(T) * C / (100 - C)   and   W = V * f   with   rho_vapour = P * M / (R * T)",
    reference = "NFPA 12 total flooding concept: agent mass = enclosure volume x flooding factor, with the flooding factor set by the design concentration for the hazard.",
    notes = "Pick the hazard class to set the design concentration (34 % for surface fires, 50 % for deep-seated), or enter your own value in the override field. The flooding factor is computed from the CO2 vapour density at the design temperature, so no flooding-factor table is embedded. The cylinder count uses the standard 45 kg charge unless you change it. CO2 concentrations are life threatening and the standard's venting, pressure-relief, pipe-sizing and personnel-safety requirements are separate steps.",
    keywords = listOf("co2", "carbon dioxide", "total flooding", "nfpa 12", "fire", "suppression", "flooding factor", "gas", "hazard class", "deep seated", "cylinder"),
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
            "mcyl", "Charge per cylinder (standard 45 kg)", "mcyl", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kg",
            defaultValue = STANDARD_CYLINDER_CHARGE,
        ),
    ),
)

object Co2AgentQuantityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val volume = value(inputs, "v") // m3
        val hazard = HazardClasses[value(inputs, "hazard").toInt()]
        val concentrationPct = if (has(inputs, "c")) value(inputs, "c") * 100.0 else hazard.second
        val cFraction = concentrationPct / 100.0
        val temperatureK = value(inputs, "t") // K
        val charge = optionalValue(inputs, "mcyl", STANDARD_CYLINDER_CHARGE)

        val vapourDensity = P_STD * M_CO2 / (R_GAS * temperatureK) // kg/m3
        val ratio = cFraction / (1.0 - cFraction)
        val floodingFactor = vapourDensity * ratio // kg/m3
        val mass = volume * floodingFactor // kg
        val massLb = mass / 0.45359237
        val cylinders = ceil(mass / charge)

        val warnings = buildList {
            add("CO2 design concentrations are life threatening: the applicable standard's requirements for occupant evacuation, alarms, warning signs, ventilation and lockout must be met before commissioning.")
            add("The class concentration is a typical design value - confirm the design concentration and the design method for the actual hazard with NFPA 12.")
            add("This is the agent quantity inside the protected volume only; the standard's compensating additions (enclosure temperature, leakage) and the pipe/nozzle calculation are separate steps.")
        }

        return CalcOutput(
            results = listOf(
                result("w", "CO2 quantity (kg)", mass, "kg", isPrimary = true),
                result("wLb", "CO2 quantity (imperial)", massLb, "lb", isPrimary = true),
                result("f", "Flooding factor", floodingFactor, "kgm3", isPrimary = true),
                result("fLb", "Flooding factor (imperial)", floodingFactor / 16.0184634, "lbft3"),
                result("cUsed", "Design concentration used", concentrationPct, "pct"),
                result("rhoVapour", "CO2 vapour density at design T", vapourDensity, "kgm3"),
                result("cylinders", "Cylinders required (${Fmt.n(charge, 3)} kg each)", cylinders, "dash"),
            ),
            steps = listOf(
                "Hazard class: ${hazard.first.label} -> C = ${Fmt.n(concentrationPct, 4)} %" +
                    if (has(inputs, "c")) " (override entered)" else "",
                "V = ${Fmt.n(volume, 4)} m3   T = ${Fmt.n(temperatureK, 4)} K",
                "rho_vapour = P * M / (R * T) = ${Fmt.n(P_STD, 6)} x ${Fmt.n(M_CO2, 5)} / (${Fmt.n(R_GAS, 5)} x ${Fmt.n(temperatureK, 4)}) = ${Fmt.n(vapourDensity, 6)} kg/m3",
                "C / (100 - C) = ${Fmt.n(concentrationPct, 4)} / ${Fmt.n(100.0 - concentrationPct, 4)} = ${Fmt.n(ratio, 6)}",
                "f = rho_vapour * C / (100 - C) = ${Fmt.n(vapourDensity, 6)} x ${Fmt.n(ratio, 6)} = ${Fmt.n(floodingFactor, 6)} kg/m3",
                "W = V * f = ${Fmt.n(volume, 4)} x ${Fmt.n(floodingFactor, 6)} = ${Fmt.n(mass, 3)} kg",
                "Cylinders = ceil(W / ${Fmt.n(charge, 3)} kg) = ${Fmt.n(cylinders, 0)}",
            ),
            warnings = warnings,
        )
    }
}
