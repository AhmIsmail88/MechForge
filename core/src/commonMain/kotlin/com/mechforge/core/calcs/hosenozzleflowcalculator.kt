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

/** Fire hose / nozzle flow: Q[gpm] = 29.7 * d[in]^2 * sqrt(P[psi]). */
private val Def = CalculatorDefinition(
    id = "hose-nozzle-flow",
    name = "Hose / Nozzle Flow",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "Water flow from a fire hose nozzle from the nozzle diameter and the nozzle pressure.",
    formulaDisplay = "Q[gpm] = 29.7 * C * d[in]^2 * sqrt(P[psi])",
    reference = "Standard fire-service nozzle discharge formula (Q = 29.7*d^2*sqrt(P), gpm/in/psi).",
    notes = "The 29.7 coefficient is defined in gpm, inches and psi; SI values shown are exact conversions. Nozzle pressure is the pressure AT the nozzle and hose friction loss is a separate calculation. C is the nozzle discharge coefficient (1.0 is the ideal smooth-bore reference); take C from the nozzle manufacturer for fog/combination nozzles.",
    keywords = listOf("hose", "nozzle", "fire", "flow", "discharge", "firefighting"),
    inputs = listOf(
        InputSpec("d", "Nozzle diameter", "d", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "in"),
        InputSpec("p", "Nozzle pressure", "P", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "psi"),
          InputSpec(
              "c", "Nozzle discharge coefficient", "C", UnitFamily.DIMENSIONLESS,
              required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0,
              defaultUnitId = "dash", defaultValue = 1.0, assumedWhenOmitted = "Nozzle coefficient assumed as 1.0 (ideal smooth-bore reference) - verify the nozzle manufacturer data."
          ),
    ),
)

object HoseNozzleFlowCalculator : Calculator(Def) {

    private const val COEFF = 29.7

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val dIn = value(inputs, "d") / 0.0254
        val pPsi = value(inputs, "p") / 6894.757293168361

          val nozzleCoeff = optionalValue(inputs, "c", 1.0)
          val qGpm = COEFF * nozzleCoeff * dIn * dIn * sqrt(pPsi)
        val qLmin = qGpm * 3.785411784
        val qM3h = qLmin * 60.0 / 1000.0

        return CalcOutput(
            results = listOf(
                result("q", "Nozzle Flow (imperial)", qGpm, "gpm", isPrimary = true),
                result("qLmin", "Nozzle Flow (L/min)", qLmin, "lmin", isPrimary = true),
                result("qM3h", "Nozzle Flow (m3/h)", qM3h, "m3h"),
            ),
            steps = listOf(
                "d = ${Fmt.n(dIn, 3)} in   P = ${Fmt.n(pPsi, 1)} psi",
                "sqrt(P) = ${Fmt.n(sqrt(pPsi), 4)}",
                  "Q = 29.7 * C * d^2 * sqrt(P) = 29.7 x ${Fmt.n(nozzleCoeff, 3)} x ${Fmt.n(dIn * dIn, 4)} x ${Fmt.n(sqrt(pPsi), 4)} = ${Fmt.n(qGpm, 2)} gpm",
                "= ${Fmt.n(qLmin, 1)} L/min = ${Fmt.n(qM3h, 2)} m3/h",
            ),
            stepsAr = listOf(
                "d = ${Fmt.n(dIn, 3)} in   P = ${Fmt.n(pPsi, 1)} psi",
                "√(P) = ${Fmt.n(sqrt(pPsi), 4)}",
                "Q = 29.7 × C × d² × √(P) = 29.7 × ${Fmt.n(nozzleCoeff, 3)} × ${Fmt.n(dIn * dIn, 4)} × ${Fmt.n(sqrt(pPsi), 4)} = ${Fmt.n(qGpm, 2)} gpm",
                "= ${Fmt.n(qLmin, 1)} L/min = ${Fmt.n(qM3h, 2)} m3/h",
            ),
            warnings = buildList {
                add("Coefficient 29.7 is the standard fire-service form (gpm, in, psi). Verify the nozzle manufacturer data for smooth-bore and fog nozzles.")
                if (pPsi < 50.0 || pPsi > 100.0) add("Nozzle pressure outside the usual 50-100 psi (3.5-7 bar) band - check it against the nozzle and pump ratings.")
            },
            warningsAr = buildList {
                add("المعامل 29.7 هو الصيغة القياسية لخدمة الحريق (gpm، بوصة، psi). تحقق من بيانات مُصنّع الفوهة للفوهات الملساء والضبابية.")
                if (pPsi < 50.0 || pPsi > 100.0) {
                    add("ضغط الفوهة خارج المدى المعتاد 50-100 psi (3.5-7 bar) - راجعه مقابل تصنيفات الفوهة والمضخة.")
                }
            },
        )
    }
}
