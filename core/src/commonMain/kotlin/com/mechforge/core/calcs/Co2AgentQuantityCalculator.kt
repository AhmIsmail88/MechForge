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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Carbon dioxide (CO2) total flooding agent quantity - NFPA 12 design route.
 *
 *     W_basic = V_net * f
 *     W_final = W_basic + W_additional          (additional entered, never a blanket %)
 *     cylinders = ceil(W_final / cylinder charge)
 *
 * The flooding factor f is the NFPA 12 design data. The calculator therefore:
 *
 *  1. cross-checks the net protected volume against the gross and excluded volumes;
 *  2. takes f from the NFPA 12 table / listed value when the engineer enters it, and
 *     otherwise shows the ideal-gas equivalent (f = rho_vapour * C/(100-C)) clearly
 *     labelled as a theoretical estimate - not as a table value;
 *  3. keeps the basic quantity, the additional quantity (unclosable openings and other
 *     applicable corrections) and the final quantity as separate numbers;
 *  4. never adds a leakage, piping or reserve percentage, and never inflates the
 *     cylinder count: piping/network design is a separate engineered calculation.
 *
 * Nothing here claims NFPA 12 compliance: the hazard classification, the design
 * concentration, the flooding factor, the edition in force and the listed system
 * configuration remain the engineer's responsibility.
 */
private const val R_GAS = 8.31446261815324 // J/(mol*K)
private const val P_STD = 101325.0 // Pa
private const val M_CO2 = 0.04401 // kg/mol
private const val STANDARD_CYLINDER_CHARGE = 45.0 // kg

/** Hazard -> design concentration (% by volume) for CO2 total flooding. */
private val HazardClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("a", "Class A - surface fire, solid combustibles (34 %)") to 34.0,
    InputOption("b", "Class B - flammable liquids (34 %)") to 34.0,
    InputOption("c", "Class C - energized electrical, surface fire (34 %)") to 34.0,
    InputOption("ds", "Class A - deep-seated (smouldering) fire (50 %)") to 50.0,
    InputOption("dse", "Deep-seated dry electrical hazard (50 %)") to 50.0,
)

private val Def = CalculatorDefinition(
    id = "co2-agent-quantity",
    name = "CO2 Total Flooding Quantity (NFPA 12)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "CO2 total flooding system in the NFPA 12 order: net protected volume, hazard classification, design concentration, the applicable flooding factor, the basic quantity, separately entered additional quantity, and the cylinder selection.",
    formulaDisplay = "W_basic = V_net * f   then   W_final = W_basic + W_additional     (f = NFPA 12 flooding factor; ideal-gas estimate f = rho_vapour * C / (100 - C))",
    reference = "NFPA 12 total flooding: agent mass = net enclosure volume x flooding factor, with the flooding factor and design concentration taken from the standard (table/listed data) for the hazard classification. State the edition in force in the report.",
    notes = "Step 1: net protected volume = gross volume - excluded (non-protected) volume; enter the gross volume too and the calculator cross-checks the net value and prints the equivalent room size, which catches a misplaced decimal point. Step 2-3: the hazard classification sets the design concentration (editable) - 34 % for surface fires, 50 % for deep-seated and for deep-seated dry electrical hazards. Step 4: the flooding factor is the NFPA 12 design data - enter the applicable table/listed value in the f field; if it is left empty the ideal-gas equivalent is shown and labelled as a theoretical estimate. Step 5-6: W_basic = V_net x f, plus an additional quantity entered for the applicable special conditions (unclosable openings and similar); no leakage, piping or reserve percentage is ever added automatically.",
    keywords = listOf("co2", "carbon dioxide", "total flooding", "nfpa 12", "fire", "suppression", "flooding factor", "gas", "hazard class", "deep seated", "deep-seated dry electrical", "cylinder", "unclosable openings"),
    inputs = listOf(
        InputSpec("v", "Net protected volume (gross - excluded)", "V_net", UnitFamily.VOLUME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("vgross", "Gross room volume (for the volume check)", "V_gross", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("vexcl", "Excluded / non-protected volume", "V_excluded", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec(
            "hazard", "Hazard classification (sets the design concentration)", "Hazard", UnitFamily.DIMENSIONLESS,
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
            "ftable", "Flooding factor from the NFPA 12 table / listed data", "f", UnitFamily.DENSITY,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3",
        ),
        InputSpec(
            "addkg", "Additional CO2 - unclosable openings / applicable corrections", "W_add", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "kg", defaultValue = 0.0,
        ),
        InputSpec(
            "mcyl", "Cylinder charge (standard 45 kg)", "m_cyl", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kg",
            defaultValue = STANDARD_CYLINDER_CHARGE,
        assumedWhenOmitted = "Cylinder charge assumed as 45 kg (standard NFPA 12 cylinder) - confirm the cylinder size actually ordered."),
    ),
)

object Co2AgentQuantityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val volume = value(inputs, "v") // m3
        val gross = if (has(inputs, "vgross")) value(inputs, "vgross") else null
        val hazard = HazardClasses[value(inputs, "hazard").toInt()]
        val concentrationPct = if (has(inputs, "c")) value(inputs, "c") * 100.0 else hazard.second
        val cFraction = concentrationPct / 100.0
        val temperatureK = value(inputs, "t") // K
        val charge = optionalValue(inputs, "mcyl", STANDARD_CYLINDER_CHARGE)
        val additional = optionalValue(inputs, "addkg", 0.0)
        val fEntered = has(inputs, "ftable")

        val vapourDensity = P_STD * M_CO2 / (R_GAS * temperatureK) // kg/m3
        val ratio = cFraction / (1.0 - cFraction)
        val fIdeal = vapourDensity * ratio // kg/m3 - theoretical estimate
        val floodingFactor = if (fEntered) value(inputs, "ftable") else fIdeal

        val basic = volume * floodingFactor // kg
        val final = basic + additional // kg
        val finalLb = final / 0.45359237
        val cylinders = ceil(final / charge)
        val installed = cylinders * charge

        val excludedFromGross = if (gross != null) gross - volume else null
        val netFromGross = if (gross != null && has(inputs, "vexcl")) gross - value(inputs, "vexcl") else null
        val side = volume.pow(1.0 / 3.0)

        val results = buildList {
            add(result("vnet", "Net protected volume used", volume, "m3"))
            if (gross != null) add(result("vexcl", "Excluded volume (gross - net)", (excludedFromGross ?: 0.0).coerceAtLeast(0.0), "m3"))
            add(result("cUsed", "Design concentration used", concentrationPct, "pct"))
            add(result("f", "Flooding factor used", floodingFactor, "kgm3", isPrimary = true))
            add(result("fIdeal", "Ideal-gas equivalent flooding factor", fIdeal, "kgm3"))
            add(result("fLb", "Flooding factor used (imperial)", floodingFactor / 16.0184634, "lbft3"))
            add(result("rhoVapour", "CO2 vapour density at design T", vapourDensity, "kgm3"))
            add(result("wbasic", "Basic CO2 quantity W_basic", basic, "kg"))
            add(result("wadd", "Additional quantity W_add", additional, "kg"))
            add(result("w", "Final required quantity W_final", final, "kg", isPrimary = true))
            add(result("wLb", "Final quantity (imperial)", finalLb, "lb", isPrimary = true))
            add(result("cylinders", "Cylinders required (${Fmt.n(charge, 1)} kg each)", cylinders, "dash"))
            add(result("installed", "Installed CO2 capacity", installed, "kg"))
            add(result("margin", "Capacity margin", installed - final, "kg"))
            add(result("marginPct", "Capacity margin", (installed - final) / final * 100.0, "pct"))
        }

        val steps = buildList {
            add("Step 1  Volumes: net V = ${Fmt.n(volume, 3)} m3" +
                (if (gross != null) "   gross = ${Fmt.n(gross, 3)} m3   excluded = ${Fmt.n((excludedFromGross ?: 0.0).coerceAtLeast(0.0), 3)} m3" else "") +
                "   (equivalent room size about ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} m)")
            add("Step 2  Agent: carbon dioxide (CO2), M = ${Fmt.n(M_CO2, 5)} kg/mol, design temperature ${Fmt.n(temperatureK, 3)} K")
            add("Step 3  Hazard: ${hazard.first.label} -> design concentration C = ${Fmt.n(concentrationPct, 3)} %" +
                if (has(inputs, "c")) " (entered)" else " (from the hazard classification)")
            add(
                if (fEntered) {
                    "Step 4  Flooding factor f = ${Fmt.n(floodingFactor, 4)} kg/m3 (entered: NFPA 12 table / listed data). Ideal-gas equivalent for reference: ${Fmt.n(fIdeal, 4)} kg/m3"
                } else {
                    "Step 4  Flooding factor f = ${Fmt.n(floodingFactor, 4)} kg/m3 (ideal-gas estimate: f = rho_vapour x C/(100-C) = ${Fmt.n(vapourDensity, 4)} x ${Fmt.n(ratio, 5)}). Enter the NFPA 12 table value when it is available."
                },
            )
            add("Step 5  W_basic = V_net x f = ${Fmt.n(volume, 3)} x ${Fmt.n(floodingFactor, 4)} = ${Fmt.n(basic, 2)} kg")
            add(
                if (additional <= 0.0) {
                    "Step 6  Additional CO2 quantity = 0 kg (no unclosable openings or other applicable corrections entered)"
                } else {
                    "Step 6  Additional CO2 quantity = ${Fmt.n(additional, 2)} kg (entered for the stated condition)"
                },
            )
            add("Step 7  W_final = W_basic + W_add = ${Fmt.n(basic, 2)} + ${Fmt.n(additional, 2)} = ${Fmt.n(final, 2)} kg")
            add("Step 8  Cylinders = CEILING(${Fmt.n(final, 2)} / ${Fmt.n(charge, 1)} kg) = ${Fmt.n(cylinders, 0)} x ${Fmt.n(charge, 1)} kg = ${Fmt.n(installed, 1)} kg installed, margin ${Fmt.n(installed - final, 2)} kg (${Fmt.n((installed - final) / final * 100.0, 1)} %)")
            add("Step 9  Assumptions: NFPA 12 methodology; state the edition in force for the project (Settings > Report details > Code / edition), and confirm the design concentration and flooding factor with the standard and the system manufacturer.")
        }

        val warnings = buildList {
            add("CO2 design concentrations are life threatening: the applicable standard's requirements for occupant evacuation, alarms, warning signs, ventilation and lockout must be met before commissioning.")
            if (netFromGross != null && abs(netFromGross - volume) > 0.02 * volume) {
                add("Volume check: gross - excluded = ${Fmt.n(netFromGross, 3)} m3 does not match the entered net volume ${Fmt.n(volume, 3)} m3. Correct the net volume before using the result.")
            }
            if (gross != null && volume > gross) {
                add("Volume check: the net volume is larger than the gross room volume - check the decimal point (a 100x slip gives a 100x agent quantity).")
            }
            if (!fEntered) {
                add("The flooding factor shown is a theoretical ideal-gas value. Where the applicable NFPA 12 table/listed flooding factor exists, enter it in the f field and use that value for the design.")
            }
            add("No additional allowance is added automatically: no leakage %, no piping %, no reserve. Any additional CO2 must come from the actual NFPA 12 provisions and the actual enclosure conditions (for example unclosable openings), entered separately - 0 kg when none apply.")
            add("The cylinder count comes from W_final and the cylinder charge; it is not inflated by an allowance. Piping/network design, discharge time, pressure relief and venting are separate engineered-system items.")
            add("Confirm the design concentration and the applicable flooding factor for the hazard with the NFPA 12 edition in force.")
        }

        return CalcOutput(results = results, steps = steps, warnings = warnings)
    }
}
