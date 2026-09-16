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

/**
 * Ventilation airflow from a target air-change rate (fan-capacity sizing):
 *
 *   ACH = Q*3600/V ;  Q = ACH*V/3600 ;  V = Q*3600/ACH
 *
 * Enter any TWO of airflow (Q), room volume (V) or air changes per hour (ACH);
 * the third is computed. The primary result is the fan/airflow capacity.
 */
private val Def = CalculatorDefinition(
    id = "air-changes-hour",
    name = "Ventilation Airflow (Fan Capacity / ACH)",
    category = CalculatorCategory.HVAC,
    description = "Required ventilation airflow (fan capacity) from a target air-change rate and room volume, or the reverse: ACH from airflow.",
    formulaDisplay = "Q = ACH*V/3600 ;  ACH = Q*3600/V ;  V = Q*3600/ACH",
    reference = "Standard ventilation metric; the required ACH comes from the applicable code or design brief - this tool only sizes the airflow.",
    notes = "Enter any TWO of Q, V or ACH; the third is computed. Typical ACH (practice guidance - verify against the applicable code): 2-6 comfort spaces, 6-12 toilets/kitchens, 10-15 plant rooms and equipment spaces.",
    keywords = listOf("ach", "air changes", "ventilation", "fan capacity", "airflow", "fresh air", "room", "supply air"),
    inputs = listOf(
        InputSpec("q", "Airflow (fan capacity)", "Q", UnitFamily.FLOW, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("vroom", "Room volume", "V", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("ach", "Air changes per hour", "ACH", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "perh"),
        InputSpec("fancap", "Fan capacity (per fan)", "Q_fan", UnitFamily.FLOW, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("nfans", "Number of fans installed", "n_fan", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "dash"),
    ),
)

object AirChangesCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val hasQ = has(inputs, "q")
        val hasV = has(inputs, "vroom")
        val hasAch = has(inputs, "ach")

        if (listOf(hasQ, hasV, hasAch).count { it } != 2) {
            throw ValidationException(
                listOf(
                    InputError(
                        "q",
                        "Enter exactly TWO of airflow (Q), room volume (V) or air changes per hour (ACH) - the third is computed.",
                    )
                )
            )
        }

        val q: Double // m3/s
        val v: Double // m3
        val ach: Double // 1/h
        when {
            hasQ && hasV -> {
                q = value(inputs, "q")
                v = value(inputs, "vroom")
                ach = q * 3600.0 / v
            }
            hasV && hasAch -> {
                v = value(inputs, "vroom")
                ach = value(inputs, "ach")
                q = ach * v / 3600.0
            }
            else -> {
                q = value(inputs, "q")
                ach = value(inputs, "ach")
                v = q * 3600.0 / ach
            }
        }

        val minutesPerChange = 60.0 / ach

        // the fan installation the engineer intends to use, sized against the airflow above
        val fanCap = if (has(inputs, "fancap")) value(inputs, "fancap") else null
        val fanCount = if (has(inputs, "nfans")) value(inputs, "nfans") else null
        val fanProvided = FanCoverage.provided(fanCount, fanCap)
        val fansNeeded = FanCoverage.fansNeeded(q, fanCap)
        val fanMargin = FanCoverage.marginPercent(fanProvided, q)

        val fanResults = buildList {
            fansNeeded?.let {
                add(result("fansNeeded", "Fans Required", it.toDouble(), "dash", isPrimary = true))
            }
            fanProvided?.let {
                add(result("fanTotal", "Installed Fan Capacity", it * 3600.0, "m3h"))
                add(result("fanTotalCfm", "Installed Fan Capacity (imperial)", it / 4.719474432e-4, "cfm"))
            }
            fanMargin?.let {
                add(result("fanMargin", "Installed Capacity Margin", it, "pct"))
            }
        }

        return CalcOutput(
            results = listOf(
                result("q", "Required Airflow (Fan Capacity)", q * 3600.0, "m3h", isPrimary = true),
                result("qCfm", "Required Airflow (imperial)", q / 4.719474432e-4, "cfm", isPrimary = true),
                result("qLs", "Required Airflow (L/s)", q * 1000.0, "ls"),
                result("ach", "Air Changes per Hour", ach, "perh", isPrimary = true),
                result("vroom", "Room Volume", v, "m3"),
                result("time", "Time per Air Change", minutesPerChange, "min"),
            ) + fanResults,
            steps = listOf(
                if (hasQ && hasV) {
                    "Airflow: ${Fmt.n(q * 3600.0, 1)} m3/h   Room volume: ${Fmt.n(v, 2)} m3"
                } else if (hasV && hasAch) {
                    "Room volume: ${Fmt.n(v, 2)} m3   Target ACH: ${Fmt.n(ach, 2)} 1/h"
                } else {
                    "Airflow: ${Fmt.n(q * 3600.0, 1)} m3/h   ACH: ${Fmt.n(ach, 2)} 1/h"
                },
                "Q = ACH*V/3600 = ${Fmt.n(ach, 2)} x ${Fmt.n(v, 2)} / 3600 = ${Fmt.n(q, 5)} m3/s",
                "Fan capacity = ${Fmt.n(q * 3600.0, 1)} m3/h = ${Fmt.n(q / 4.719474432e-4, 0)} CFM = ${Fmt.n(q * 1000.0, 1)} L/s",
                "ACH = Q*3600/V = ${Fmt.n(q * 3600.0, 1)} / ${Fmt.n(v, 2)} = ${Fmt.n(ach, 2)} 1/h (one air change every ${Fmt.n(minutesPerChange, 1)} min)",
            ),
            warnings = buildList {
                if (ach < 2.0) {
                    add("Below 2 ACH - verify the target against the applicable ventilation requirement for this space.")
                }
                if (ach > 60.0) {
                    add("Above 60 ACH is unusual - confirm the target rate and check acoustic and pressure-drop implications.")
                }
                addAll(FanCoverage.warnings(q, fanProvided, fansNeeded, fanCount))
            },
        )
    }
}
