package com.mechforge.core.calcs

import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/**
 * Ventilation airflow for a heat load (heat-dissipation method):
 *
 *   m_dot = P / (c_p * dT) ;  Q = m_dot / rho
 *
 * The space rejects [p] watts of sensible heat; the air that leaves the space may not be warmer
 * than the room temperature plus [dt]. The mass flow that carries the heat away follows from the
 * sensible-heat equation, and dividing by the air density gives the fan capacity.
 *
 * A fan capacity and a fan count may be entered as well, in which case the tool reports how many
 * fans of that size the calculated airflow takes and how much margin the installation carries.
 */
private const val CP_AIR = 1005.0
private const val RHO_AIR = 1.2

private val Def = CalculatorDefinition(
    id = "heat-dissipation",
    name = "Heat Dissipation Airflow",
    category = CalculatorCategory.HVAC,
    description = "Ventilation airflow (fan capacity) needed to remove a sensible heat load for a chosen air temperature rise: Q = P / (rho * c_p * dT).",
    formulaDisplay = "m_dot = P/(c_p*dT) ;  Q = m_dot/rho  =>  Q = P/(rho*c_p*dT)",
    reference = "Sensible-heat equation of the ASHRAE Handbook - Fundamentals (Q = m_dot*c_p*dT), rearranged for the airflow. The design temperature rise and the heat load are project data - this tool only sizes the airflow.",
    notes = "Sensible heat only: latent (moisture) loads, radiation to surrounding surfaces and duct heat gain are not included - add them to the load before sizing the fan. Air properties are taken at the stated density and specific heat; correct them for altitude and temperature. Typical practice for equipment and plant rooms is a 5-15 K rise above ambient.",
    keywords = listOf("heat dissipation", "heat load", "ventilation", "cooling airflow", "fan capacity", "kw", "temperature rise", "plant room", "electrical room"),
    inputs = listOf(
        InputSpec("p", "Heat dissipation (sensible heat load)", "P", UnitFamily.POWER, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kw"),
        InputSpec("dt", "Allowable air temperature rise", "dT", UnitFamily.TEMPERATURE_DIFFERENCE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "delk"),
        InputSpec("rho", "Air density", "rho", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density", assumedWhenOmitted = "Air density assumed as 1.2 kg/m3 (20 C, sea level) - correct it for the actual temperature and altitude."),
        InputSpec("cp", "Specific heat of air", "c_p", UnitFamily.SPECIFIC_HEAT, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "jkgk", assumedWhenOmitted = "Specific heat assumed as 1005 J/(kg.K) (air at 20 C) - use the value for the gas actually handled."),
        InputSpec("fancap", "Fan capacity (per fan)", "Q_fan", UnitFamily.FLOW, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("nfans", "Number of fans installed", "n_fan", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "dash"),
    ),
)

object HeatDissipationCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p = value(inputs, "p") // W
        val dt = value(inputs, "dt") // K
        val rho = optionalValue(inputs, "rho", RHO_AIR) // kg/m3
        val cp = optionalValue(inputs, "cp", CP_AIR) // J/(kg.K)

        val mDot = p / (cp * dt) // kg/s
        val q = mDot / rho // m3/s
        val qM3h = q * 3600.0
        val qCfm = q / 4.719474432e-4
        val qLs = q * 1000.0

        val fanCap = if (has(inputs, "fancap")) value(inputs, "fancap") else null
        val fanCount = if (has(inputs, "nfans")) value(inputs, "nfans") else null
        val provided = FanCoverage.provided(fanCount, fanCap)
        val fansNeeded = FanCoverage.fansNeeded(q, fanCap)
        val margin = FanCoverage.marginPercent(provided, q)

        val fanResults = buildList {
            fansNeeded?.let {
                add(result("fansNeeded", "Fans Required", it.toDouble(), "dash", isPrimary = true))
            }
            provided?.let {
                add(result("fanTotal", "Installed Fan Capacity", it * 3600.0, "m3h"))
                add(result("fanTotalCfm", "Installed Fan Capacity (imperial)", it / 4.719474432e-4, "cfm"))
            }
            margin?.let {
                add(result("fanMargin", "Installed Capacity Margin", it, "pct"))
            }
        }

        return CalcOutput(
            results = listOf(
                result("q", "Required Airflow (Fan Capacity)", qM3h, "m3h", isPrimary = true),
                result("qCfm", "Required Airflow (imperial)", qCfm, "cfm", isPrimary = true),
                result("qLs", "Required Airflow (L/s)", qLs, "ls"),
                result("mdot", "Air Mass Flow", mDot, "kgs"),
                result("densityUsed", "Air Density Used", rho, "kgm3"),
                result("cpUsed", "Specific Heat Used", cp, "jkgk"),
            ) + fanResults,
            steps = listOf(
                "Heat load: P = ${Fmt.n(p / 1000.0, 3)} kW   Allowable rise: dT = ${Fmt.n(dt, 2)} K",
                "Air properties: rho = ${Fmt.n(rho, 4)} kg/m3, c_p = ${Fmt.n(cp, 1)} J/(kg.K)",
                "Mass flow: m_dot = P/(c_p*dT) = ${Fmt.n(p, 1)} / (${Fmt.n(cp, 1)} x ${Fmt.n(dt, 2)}) = ${Fmt.n(mDot, 5)} kg/s",
                "Volumetric flow: Q = m_dot/rho = ${Fmt.n(mDot, 5)} / ${Fmt.n(rho, 4)} = ${Fmt.n(q, 5)} m3/s",
                "Fan capacity = ${Fmt.n(qM3h, 1)} m3/h = ${Fmt.n(qCfm, 0)} CFM = ${Fmt.n(qLs, 1)} L/s",
                "Rise check: dT = P/(rho*c_p*Q) = ${Fmt.n(dt, 2)} K at this airflow",
            ) + buildList {
                if (fanCap != null && fansNeeded != null) {
                    add(
                        "Fans of ${Fmt.n(fanCap * 3600.0, 1)} m3/h each: " +
                            "$fansNeeded fan(s) required" +
                            (provided?.let { " (installed ${Fmt.n(it * 3600.0, 1)} m3/h)" } ?: "")
                    )
                }
            },
            warnings = buildList {
                if (dt < 5.0) {
                    add("Allowable rise below 5 K needs a large airflow - confirm the design temperature and the duct space it implies.")
                }
                if (dt > 20.0) {
                    add("Allowable rise above 20 K may overheat equipment in the space - verify the permissible room temperature, not only the airflow.")
                }
                add("Airflow is inversely proportional to the allowable rise: halving dT doubles the fan capacity.")
                addAll(FanCoverage.warnings(q, provided, fansNeeded, fanCount))
            },
        )
    }
}
