package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Tank volume from flow and detention time: V = Q·t. */
private val TankDef = CalculatorDefinition(
    id = "tank-volume",
    name = "Tank Volume (Flow × Time)",
    category = CalculatorCategory.WATER_WASTEWATER,
    description = "Required storage volume for a given flow and detention/retention time.",
    formulaDisplay = "V = Q·t",
    reference = "Continuity/volume balance (standard water and wastewater practice).",
    notes = "Volume only — freeboard, partition walls, dead storage and structural allowances must be added separately.",
    keywords = listOf("tank", "volume", "detention", "storage", "water"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("t", "Detention time", "t", UnitFamily.TIME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "h"),
    ),
)

object TankVolumeCalculator : Calculator(TankDef) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val t = value(inputs, "t")
        val v = q * t

        return CalcOutput(
            results = listOf(
                result("v", "Required Volume", v, "m3", isPrimary = true),
                result("vl", "Required Volume (litres)", v * 1000.0, "liter"),
            ),
            steps = listOf(
                "V = Q·t = ${Fmt.n(q * 3600.0, 2)} m³/h × ${Fmt.n(t / 3600.0, 3)} h = ${Fmt.n(v, 3)} m³",
                "Depth/area check: with a usable depth of 3.0 m the required plan area would be ${Fmt.n(v / 3.0, 1)} m².",
            ),
            warnings = listOf("Add freeboard and structural allowances; dead storage is not deducted here."),
        )
    }
}

/** Detention time: t = V/Q. */
private val DetentionDef = CalculatorDefinition(
    id = "detention-time",
    name = "Detention Time",
    category = CalculatorCategory.WATER_WASTEWATER,
    description = "Detention (retention) time of a tank or basin from its volume and throughflow.",
    formulaDisplay = "t = V / Q",
    reference = "Standard process calculation for tanks, clarifiers and basins.",
    notes = "Theoretical (nominal) detention time. Actual time is shorter due to short-circuiting; tracer tests give the real value.",
    keywords = listOf("detention time", "retention", "basin", "clarifier", "wastewater"),
    inputs = listOf(
        InputSpec("v", "Tank / basin volume", "V", UnitFamily.VOLUME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("q", "Throughflow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
    ),
)

object DetentionTimeCalculator : Calculator(DetentionDef) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val v = value(inputs, "v")
        val q = value(inputs, "q")
        val t = v / q

        return CalcOutput(
            results = listOf(
                result("t", "Detention Time", t / 3600.0, "h", isPrimary = true),
                result("tm", "Detention Time (minutes)", t / 60.0, "min"),
            ),
            steps = listOf(
                "t = V/Q = ${Fmt.n(v, 2)} m³ / ${Fmt.n(q * 3600.0, 2)} m³/h = ${Fmt.n(t / 3600.0, 3)} h (${Fmt.n(t / 60.0, 1)} min)",
            ),
            warnings = listOf("Nominal detention time — actual time is reduced by short-circuiting."),
        )
    }
}

/** Chlorine / chemical dosing: mass rate = Q·dose. */
private val DosingDef = CalculatorDefinition(
    id = "chlorine-dose",
    name = "Chlorine / Chemical Dose",
    category = CalculatorCategory.WATER_WASTEWATER,
    description = "Chemical mass feed rate from water flow and target dose (mg/L).",
    formulaDisplay = "ṁ = Q·dose",
    reference = "Mass-balance dosing relation; dose values come from the disinfection/process requirement.",
    notes = "Enter the dose in mg/L (equivalent to g/m³). This computes the pure chemical mass rate — commercial product strength (e.g. 12% hypochlorite, 65% HTH) must be converted separately.",
    keywords = listOf("chlorine", "dosing", "dose", "disinfection", "chemical", "water"),
    inputs = listOf(
        InputSpec("q", "Water flow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3d"),
        InputSpec("dose", "Target dose", "dose", UnitFamily.DENSITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mgl"),
    ),
)

object ChlorineDoseCalculator : Calculator(DosingDef) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val dose = value(inputs, "dose") // kg/m³
        val massRate = q * dose // kg/s

        return CalcOutput(
            results = listOf(
                result("mr", "Chemical Mass Rate", massRate * 86400.0, "kgd", isPrimary = true),
                result("mh", "Chemical Mass Rate (kg/h)", massRate * 3600.0, "kgh"),
            ),
            steps = listOf(
                "Flow: ${Fmt.n(q * 86400.0, 0)} m³/d   Dose: ${Fmt.n(dose * 1e6, 3)} mg/L",
                "ṁ = Q·dose = ${Fmt.n(q, 6)} m³/s × ${Fmt.n(dose, 8)} kg/m³ = ${Fmt.n(massRate, 8)} kg/s = ${Fmt.n(massRate * 86400.0, 2)} kg/d",
            ),
            warnings = listOf(
                "Mass of PURE chemical. Convert to product mass using its strength (e.g. ÷0.12 for 12% NaOCl solution).",
                "Dose and contact-time requirements come from the applicable disinfection standard — verify CT compliance separately.",
            ),
        )
    }
}

/** Peak flow from a peaking factor. */
private val PeakFlowDef = CalculatorDefinition(
    id = "peak-flow",
    name = "Peak Flow (Peaking Factor)",
    category = CalculatorCategory.WATER_WASTEWATER,
    description = "Design peak flow from average flow and a peaking factor.",
    formulaDisplay = "Q_peak = PF · Q_average",
    reference = "Standard sanitary/water design practice; empirical peaking-factor formulae exist (Harmon, Babbit) and are project-dependent.",
    notes = "Typical PF ranges 1.5–3.0 for municipal wastewater depending on population and collection system (practice guidance — verify against the applicable design criteria).",
    keywords = listOf("peak flow", "peaking factor", "design flow", "wastewater", "surcharge"),
    inputs = listOf(
        InputSpec("q", "Average flow", "Q_avg", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("pf", "Peaking factor", "PF", UnitFamily.DIMENSIONLESS, minValue = 1.0, exclusiveMin = false, defaultUnitId = "dash"),
    ),
)

object PeakFlowCalculator : Calculator(PeakFlowDef) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val pf = value(inputs, "pf")
        val peak = q * pf

        return CalcOutput(
            results = listOf(
                result("qp", "Peak Flow", peak * 3600.0, "m3h", isPrimary = true),
                result("qpd", "Peak Flow (m³/d)", peak * 86400.0, "m3d"),
            ),
            steps = listOf(
                "Q_peak = PF · Q_avg = ${Fmt.n(pf, 3)} × ${Fmt.n(q * 3600.0, 2)} m³/h = ${Fmt.n(peak * 3600.0, 2)} m³/h",
                "Daily equivalent: ${Fmt.n(peak * 86400.0, 0)} m³/d",
            ),
            warnings = buildList {
                add("Peaking factor is a design input — confirm it against the applicable design criteria or a population-based formula.")
                if (pf > 4.0) add("Peaking factor above 4.0 is unusually high; verify the source of the value.")
            },
        )
    }
}

/** Hydraulic loading rate: HLR = Q/A. */
private val HydraulicLoadingDef = CalculatorDefinition(
    id = "hydraulic-loading",
    name = "Hydraulic Loading Rate",
    category = CalculatorCategory.WATER_WASTEWATER,
    description = "Surface/plan hydraulic loading rate of a tank, filter or basin from flow and area.",
    formulaDisplay = "HLR = Q / A",
    reference = "Standard process loading-rate definition for clarifiers, filters and basins.",
    notes = "Computed in m/d (and m/h). Acceptable loading rates are process- and code-specific and must be verified separately.",
    keywords = listOf("hydraulic loading", "surface loading", "overflow rate", "filter", "clarifier"),
    inputs = listOf(
        InputSpec("q", "Flow rate", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3d"),
        InputSpec("a", "Surface area", "A", UnitFamily.AREA, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m2"),
    ),
)

object HydraulicLoadingCalculator : Calculator(HydraulicLoadingDef) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q") // m³/s
        val a = value(inputs, "a") // m²
        val hlr = q / a // m/s

        return CalcOutput(
            results = listOf(
                result("hlr", "Hydraulic Loading Rate", hlr * 86400.0, "md", isPrimary = true),
                result("hlrh", "Hydraulic Loading Rate (m/h)", hlr * 3600.0, "mh"),
            ),
            steps = listOf(
                "Flow: ${Fmt.n(q * 86400.0, 0)} m³/d   Area: ${Fmt.n(a, 2)} m²",
                "HLR = Q/A = ${Fmt.n(q, 6)} m³/s / ${Fmt.n(a, 2)} m² = ${Fmt.n(hlr * 86400.0, 3)} m/d (${Fmt.n(hlr * 3600.0, 3)} m/h)",
            ),
            warnings = listOf("Acceptable loading rates are process-specific — verify against the design criteria for this unit."),
        )
    }
}
