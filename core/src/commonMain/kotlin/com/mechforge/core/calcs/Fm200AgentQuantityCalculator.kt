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
import kotlin.math.pow

/**
 * HFC-227ea (FM-200) total flooding agent quantity - NFPA 2001 design route.
 *
 *     W_basic = (V_net / S) * (C / (100 - C))
 *     W_final = W_basic + W_additional          (additional entered, never a blanket %)
 *     cylinders = ceil(W_final / listed cylinder charge)
 *
 * The calculation is deliberately split the way NFPA 2001 presents it:
 *
 *  1. V_net = gross volume - excluded volume;
 *  2. the agent and the hazard classification decide the design concentration;
 *  3. S is the specific vapour volume of the agent - use the NFPA 2001 / manufacturer
 *     value when you have it (the field overrides the ideal-gas estimate shown here);
 *  4. the basic quantity is added to a *separately entered* additional quantity: the
 *     software never adds a blanket leakage/piping percentage, because piping and
 *     network design are a separate engineered calculation;
 *  5. the cylinder selection is based on the listed cylinder charge, and the resulting
 *     capacity margin is reported rather than described as a piping allowance.
 *
 * Nothing here claims NFPA 2001 compliance: the method, the design concentration, the
 * agent property data, the edition in force and the listed-system configuration remain
 * the engineer's responsibility.
 */
private const val R_GAS = 8.31446261815324 // J/(mol*K)
private const val P_STD = 101325.0 // Pa
private const val M_HFC_227EA = 0.17003 // kg/mol

/**
 * Hazard classification -> design concentration (% by volume) for HFC-227ea.
 *
 * These are the commonly used minimum design concentrations for the agent; NFPA 2001
 * derives them from the fuel data and the edition in force, and the manufacturer's
 * listed design manual governs a specific hazard. They are editable in the calculator.
 */
private val HazardClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("a", "Class A - surface fire, solid combustibles (7.0 %)") to 7.0,
    InputOption("b", "Class B - flammable liquids (8.7 %)") to 8.7,
    InputOption("c", "Class C - energized electrical / electronic (7.0 %)") to 7.0,
)

private val Def = CalculatorDefinition(
    id = "fm200-agent-quantity",
    name = "FM-200 (HFC-227ea) Total Flooding Quantity",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "FM-200 clean agent for a total flooding system, in the NFPA 2001 order: net protected volume, hazard classification, design concentration, basic quantity, separately entered additional quantity, and the cylinder selection.",
    formulaDisplay = "W_basic = (V_net / S) * (C / (100 - C))   then   W_final = W_basic + W_additional",
    reference = "NFPA 2001 total flooding relation W = (V/S)*(C/(100-C)). Design concentrations and specific vapour volumes are code and manufacturer data - take them from the edition in force and the agent manufacturer's listed design manual, and state the edition in the report.",
    notes = "Step 1: net protected volume = gross volume - excluded (non-protected) volume; enter the gross volume as well and the calculator cross-checks the net value and prints the equivalent room size, which catches a misplaced decimal point. Step 2-3: the hazard classification sets the design concentration (editable). Step 4: S is the specific vapour volume of the agent at the design temperature; the ideal-gas estimate is shown for reference, and the field overrides it with the NFPA 2001 / manufacturer value whenever you have it. Step 5-6: the basic quantity and a SEPARATELY ENTERED additional quantity for applicable special conditions. The software never adds a blanket leakage or piping percentage, and it never inflates the cylinder count: piping/nozzle content, network design and any reserve are separate engineered-system items.",
    keywords = listOf("fm-200", "fm200", "hfc-227ea", "hfc 227ea", "clean agent", "total flooding", "nfpa 2001", "fire", "suppression", "gaseous", "hazard class", "class a", "class b", "class c", "design concentration", "specific vapour volume"),
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
            "s", "Specific vapour volume - NFPA 2001 / listed value", "S", UnitFamily.SPECIFIC_VOLUME,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3perkg",
        ),
        InputSpec(
            "addkg", "Additional quantity for applicable special conditions", "W_add", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "kg", defaultValue = 0.0,
        ),
        InputSpec(
            "mcyl", "Listed cylinder charge", "m_cyl", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kg",
        ),
    ),
)

object Fm200AgentQuantityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val volume = value(inputs, "v") // m3
        val gross = if (has(inputs, "vgross")) value(inputs, "vgross") else null
        val excluded = if (has(inputs, "vexcl")) value(inputs, "vexcl") else null
        val hazard = HazardClasses[value(inputs, "hazard").toInt()]
        val concentrationPct = if (has(inputs, "c")) value(inputs, "c") * 100.0 else hazard.second
        val cFraction = concentrationPct / 100.0
        val temperatureK = value(inputs, "t") // K
        val sDerived = !has(inputs, "s")
        val s = if (sDerived) R_GAS * temperatureK / (P_STD * M_HFC_227EA) else value(inputs, "s")
        val additional = optionalValue(inputs, "addkg", 0.0)

        val ratio = cFraction / (1.0 - cFraction)
        val basic = (volume / s) * ratio // kg
        val final = basic + additional // kg
        val finalLb = final / 0.45359237
        val equivalentFactor = basic / volume // kg/m3 - reported as the equivalent factor
        val vapourVolume = basic * s // m3 of agent vapour

        // Volume cross-checks (the 4704 vs 47.04 class of mistake shows up here).
        val excludedFromGross = if (gross != null) gross - volume else null
        val netFromGross = if (gross != null && excluded != null) gross - excluded else null
        val side = volume.pow(1.0 / 3.0)

        val results = buildList {
            add(result("vnet", "Net protected volume used", volume, "m3"))
            if (gross != null) add(result("vexcl", "Excluded volume (gross - net)", (excludedFromGross ?: 0.0).coerceAtLeast(0.0), "m3"))
            add(result("cUsed", "Design concentration used", concentrationPct, "pct"))
            add(result("sUsed", "Specific vapour volume used", s, "m3perkg"))
            add(result("wbasic", "Basic agent quantity W_basic", basic, "kg"))
            add(result("wadd", "Additional quantity W_add", additional, "kg"))
            add(result("w", "Final required quantity W_final", final, "kg", isPrimary = true))
            add(result("wLb", "Final quantity (imperial)", finalLb, "lb", isPrimary = true))
            add(result("f", "Equivalent flooding factor (W_basic / V)", equivalentFactor, "kgm3"))
            add(result("fLb", "Equivalent flooding factor (imperial)", equivalentFactor / 16.0184634, "lbft3"))
            add(result("vapourVolume", "Agent vapour volume at design T", vapourVolume, "m3"))
            if (has(inputs, "mcyl")) {
                val charge = value(inputs, "mcyl")
                val count = ceil(final / charge)
                val installed = count * charge
                add(result("cylinders", "Cylinders required (${Fmt.n(charge, 3)} kg each)", count, "dash"))
                add(result("installed", "Installed agent capacity", installed, "kg"))
                add(result("margin", "Capacity margin", installed - final, "kg"))
                add(result("marginPct", "Capacity margin", (installed - final) / final * 100.0, "pct"))
            }
        }

        val steps = buildList {
            add("Step 1  Volumes: net V = ${Fmt.n(volume, 3)} m3" +
                (if (gross != null) "   gross = ${Fmt.n(gross, 3)} m3   excluded = ${Fmt.n((excludedFromGross ?: 0.0).coerceAtLeast(0.0), 3)} m3" else "") +
                "   (equivalent room size about ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} m)")
            add("Step 2  Agent: HFC-227ea (FM-200), M = ${Fmt.n(M_HFC_227EA, 5)} kg/mol")
            add("Step 3  Hazard: ${hazard.first.label} -> design concentration C = ${Fmt.n(concentrationPct, 3)} %" +
                if (has(inputs, "c")) " (entered)" else " (from the hazard classification)")
            add(
                if (sDerived) {
                    "Step 4  S = R*T/(P*M) = ${Fmt.n(R_GAS, 5)} x ${Fmt.n(temperatureK, 3)} / (${Fmt.n(P_STD, 6)} x ${Fmt.n(M_HFC_227EA, 5)}) = ${Fmt.n(s, 6)} m3/kg (ideal-gas estimate)"
                } else {
                    "Step 4  S = ${Fmt.n(s, 6)} m3/kg (entered - NFPA 2001 / listed value)"
                },
            )
            add("Step 5  C/(100-C) = ${Fmt.n(concentrationPct, 3)} / ${Fmt.n(100.0 - concentrationPct, 3)} = ${Fmt.n(ratio, 6)}")
            add("Step 5  W_basic = (V_net / S) x (C/(100-C)) = (${Fmt.n(volume, 3)} / ${Fmt.n(s, 6)}) x ${Fmt.n(ratio, 6)} = ${Fmt.n(basic, 2)} kg")
            add(
                if (additional <= 0.0) {
                    "Step 6  Additional quantity = 0 kg (no applicable special conditions entered)"
                } else {
                    "Step 6  Additional quantity = ${Fmt.n(additional, 2)} kg (entered for the stated special conditions)"
                },
            )
            add("Step 7  W_final = W_basic + W_add = ${Fmt.n(basic, 2)} + ${Fmt.n(additional, 2)} = ${Fmt.n(final, 2)} kg")
            if (has(inputs, "mcyl")) {
                val charge = value(inputs, "mcyl")
                val count = ceil(final / charge)
                add("Step 8  Cylinders = CEILING(${Fmt.n(final, 2)} / ${Fmt.n(charge, 3)} kg) = ${Fmt.n(count, 0)} x ${Fmt.n(charge, 3)} kg = ${Fmt.n(count * charge, 2)} kg installed")
            } else {
                add("Step 8  Enter the listed cylinder charge to get the cylinder selection and the capacity margin.")
            }
            add("Step 9  Assumptions: NFPA 2001 methodology; state the edition in force for the project (Settings > Report details > Code / edition) and confirm the concentration, S and the listed system with the manufacturer.")
        }

        val warnings = buildList {
            if (netFromGross != null && kotlin.math.abs(netFromGross - volume) > 0.02 * volume) {
                add("Volume check: gross - excluded = ${Fmt.n(netFromGross, 3)} m3 does not match the entered net volume ${Fmt.n(volume, 3)} m3. Correct the net volume before using the result.")
            }
            if (gross != null && volume > gross) {
                add("Volume check: the net volume is larger than the gross room volume - check the decimal point (a 100x slip gives a 100x agent quantity).")
            }
            if (sDerived) {
                add("S is an ideal-gas estimate. Use the specific vapour volume from the applicable NFPA 2001 edition or the manufacturer's table when it is available, and enter it in the S field.")
            }
            add("No leakage, piping or reserve allowance is added automatically. Any additional quantity must come from the actual NFPA 2001 provisions and the actual enclosure conditions, entered separately - piping/nozzle content and the network design are a separate engineered-system calculation.")
            add("Confirm the minimum design concentration for the hazard with the NFPA 2001 edition in force and the agent manufacturer's listed design manual.")
            if (hazard.first.id == "c") {
                add("Class C: for energized electrical hazards - in particular above 480 V that remain energized during and after discharge - do not assume the standard design concentration is sufficient; perform the applicable hazard analysis/testing.")
            }
            if (additional > 0.0) {
                add("The additional quantity was entered by the engineer; state its basis (unclosable openings, leakage, manufacturer's requirement) in the report.")
            }
        }

        return CalcOutput(results = results, steps = steps, warnings = warnings)
    }
}
