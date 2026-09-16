package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.sqrt

/** Sprinkler discharge from the K-factor: Q = K*sqrt(P). */
private val Def = CalculatorDefinition(
    id = "sprinkler-discharge",
    name = "Sprinkler Discharge (K-Factor)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Water discharge from a sprinkler head or spray nozzle from its K-factor and the pressure at the outlet.",
    formulaDisplay = "Q = K * sqrt(P)",
    reference = "NFPA 13 K-factor definition (Q = K*sqrt(P)); K values from the sprinkler listing.",
    notes = "K is entered in gpm/psi^0.5 or L/min/bar^0.5 (1 gpm/psi^0.5 = 14.417 L/min/bar^0.5). P is the pressure AT THE SPRINKLER outlet. This sizes the head, not the system - hydraulic calculations per NFPA 13 are a separate step.",
    keywords = listOf("sprinkler", "k-factor", "k factor", "discharge", "fire", "nfpa 13", "head"),
    inputs = listOf(
        InputSpec("k", "Sprinkler K-factor", "K", UnitFamily.FLOW_FACTOR, minValue = 0.0, exclusiveMin = true, defaultUnitId = "gpmpsi"),
        InputSpec("p", "Pressure at the sprinkler", "P", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
    ),
)

object SprinklerDischargeCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val k = value(inputs, "k")            // L/min/bar^0.5
        val pBar = value(inputs, "p") / 1e5   // bar
        val qLmin = k * sqrt(pBar)            // L/min

        val qGpm = qLmin / 3.785411784
        val qM3h = qLmin * 60.0 / 1000.0

        val warnings = buildList {
            add("K-factor tables and minimum sprinkler pressures are code data - take them from the sprinkler listing and the applicable NFPA 13 design criteria.")
        }

        return CalcOutput(
            results = listOf(
                result("q", "Discharge (L/min)", qLmin, "lmin", isPrimary = true),
                result("qGpm", "Discharge (imperial)", qGpm, "gpm", isPrimary = true),
                result("qM3h", "Discharge (m3/h)", qM3h, "m3h"),
            ),
            steps = listOf(
                "K = ${Fmt.n(k, 4)} L/min/bar^0.5   P = ${Fmt.n(pBar, 4)} bar",
                "sqrt(P) = ${Fmt.n(sqrt(pBar), 5)}",
                "Q = K * sqrt(P) = ${Fmt.n(k, 4)} x ${Fmt.n(sqrt(pBar), 5)} = ${Fmt.n(qLmin, 2)} L/min = ${Fmt.n(qGpm, 2)} gpm",
            ),
            warnings = warnings,
            stepsAr = listOf(
                "K = ${Fmt.n(k, 4)} L/min/bar^0.5   P = ${Fmt.n(pBar, 4)} bar",
                "√(P) = ${Fmt.n(sqrt(pBar), 5)}",
                "Q = K × √(P) = ${Fmt.n(k, 4)} × ${Fmt.n(sqrt(pBar), 5)} = ${Fmt.n(qLmin, 2)} L/min = ${Fmt.n(qGpm, 2)} gpm",
            ),
            warningsAr = buildList {
                add("جداول معامل K وأقل ضغوط الرشاشات بيانات كودية - خُذها من قوائم اعتماد الرشاش ومعايير تصميم NFPA 13 المطبقة.")
            },
        )
    }
}
