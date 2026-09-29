package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import kotlin.math.abs
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt

/** Latent heat of an air stream: Q_l = ρ·V̇·h_fg·Δw. */
private val Def = CalculatorDefinition(
    id = "latent-heat",
    name = "Latent Heat (Air)",
    category = CalculatorCategory.HVAC,
    description = "Latent heat from moisture change in an air stream (humidification or dehumidification).",
    formulaDisplay = "Q_l = ρ·V̇·h_fg·Δw",
    reference = "Standard air-side psychrometric relation; h_fg = 2501 kJ/kg at 0 °C (approximation).",
    notes = "Δw = w_leaving − w_entering in kg/kg dry air. Positive = moisture added (humidification).",
    keywords = listOf("latent", "moisture", "humidity", "humidification", "hvac"),
    inputs = listOf(
        InputSpec("q", "Airflow", "V̇", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("win", "Entering humidity ratio", "w_in", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("wout", "Leaving humidity ratio", "w_out", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("rho", "Air density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
    ),
)

object LatentHeatCalculator : Calculator(Def) {

    private const val H_FG = 2501.0

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val wIn = value(inputs, "win")
        val wOut = value(inputs, "wout")
        // Air density: an explicit input wins; otherwise, when the site conditions are given,
        // the density is computed from them. Without either, the 1.2 kg/m3 shorthand stays.
        val rhoFromSite = if (has(inputs, "tair") && has(inputs, "alt")) {
            airDensity(value(inputs, "tair"), value(inputs, "alt"))
        } else null
        val rho = if (has(inputs, "rho")) value(inputs, "rho") else (rhoFromSite ?: 1.2)
        val rhoSiteWarning = rhoFromSite?.let { r ->
            "Air density from " + Fmt.n(value(inputs, "tair"), 1) + " C at " + Fmt.n(value(inputs, "alt"), 0) +
                " m = " + Fmt.n(r, 4) + " kg/m3" +
                (if (abs(r - 1.2) / 1.2 > 0.05) {
                    " - differs from the 1.2 shorthand by more than 5 percent: the airflow scales with it."
                } else {
                    ""
                })
        }

        val massFlow = rho * q
        val dw = wOut - wIn
        val ql = massFlow * H_FG * dw

        val warnings = buildList {
            rhoSiteWarning?.let { add(it) }
            if (!has(inputs, "rho")) add("Air density not provided — assumed 1.2 kg/m³ (standard air ~20 °C).")
            if (dw > 0) add("Δw > 0 — moisture is ADDED to the air (humidification duty).")
            if (dw < 0) add("Δw < 0 — moisture is REMOVED from the air (dehumidification duty).")
        }

        return CalcOutput(
            results = listOf(
                result("ql", "Latent Heat", ql, "kw", isPrimary = true),
                result("dw", "Humidity Ratio Difference", dw, "dash"),
            ),
            steps = listOf(
                "Mass flow: ṁ = ρ·V̇ = ${Fmt.n(rho, 3)} × ${Fmt.n(q, 4)} = ${Fmt.n(massFlow, 4)} kg/s",
                "Δw = ${Fmt.n(wOut, 5)} − ${Fmt.n(wIn, 5)} = ${Fmt.n(dw, 5)} kg/kg",
                "Q_l = ṁ·h_fg·Δw = ${Fmt.n(massFlow, 4)} × 2501 × ${Fmt.n(dw, 5)} = ${Fmt.n(ql, 3)} kW",
            ),
            warnings = warnings,
            stepsAr = listOf(
                "معدل الكتلة: ṁ = ρ·V̇ = ${Fmt.n(rho, 3)} × ${Fmt.n(q, 4)} = ${Fmt.n(massFlow, 4)} kg/s",
                "Δw = ${Fmt.n(wOut, 5)} − ${Fmt.n(wIn, 5)} = ${Fmt.n(dw, 5)} kg/kg",
                "Q_l = ṁ·h_fg·Δw = ${Fmt.n(massFlow, 4)} × 2501 × ${Fmt.n(dw, 5)} = ${Fmt.n(ql, 3)} kW",
            ),
            warningsAr = buildList {
                if (!has(inputs, "rho")) {
                    add("لم تُدخل كثافة الهواء - افتُرضت 1.2 kg/m³ (هواء قياسي عند نحو 20 °C).")
                }
                if (dw > 0) {
                    add("Δw > 0 - الرطوبة تُضاف إلى الهواء (حمل ترطيب).")
                }
                if (dw < 0) {
                    add("Δw < 0 - الرطوبة تُزال من الهواء (حمل تجفيف).")
                }
            },
        )
    }
}
