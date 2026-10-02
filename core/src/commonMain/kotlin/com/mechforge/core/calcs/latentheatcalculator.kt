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
    notes = "Δw = w_leaving − w_entering in kg/kg dry air. Positive = moisture added (humidification). The latent heat h_fg = 2501 kJ/kg is the value at 0 C; it falls with temperature. Moist air carries its own correction to the specific heat that is not applied here.",
    keywords = listOf("latent", "moisture", "humidity", "humidification", "hvac"),
    inputs = listOf(
        InputSpec("q", "Airflow", "V̇", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3h"),
        InputSpec("win", "Entering humidity ratio", "w_in", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("wout", "Leaving humidity ratio", "w_out", UnitFamily.DIMENSIONLESS, minValue = 0.0, exclusiveMin = false, maxValue = 0.2, defaultUnitId = "dash"),
        InputSpec("rho", "Air density", "ρ", UnitFamily.DENSITY, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3", libraryKey = "density"),
                InputSpec("alt", "Site altitude (for the density)", "alt", UnitFamily.LENGTH, required = false, defaultUnitId = "m"),
                InputSpec("tair", "Air temperature (for the density)", "T_air", UnitFamily.TEMPERATURE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
    ),
)

object LatentHeatCalculator : Calculator(Def) {

    private const val H_FG = 2501.0

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val q = value(inputs, "q")
        val wIn = value(inputs, "win")
        val wOut = value(inputs, "wout")
        val air = resolveAirDensity(inputs)
        val rho = air.value

        val massFlow = rho * q
        val dw = wOut - wIn
        val ql = massFlow * H_FG * dw

        val warnings = buildList {
            addAll(air.warnings)
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
                addAll(air.warningsAr)
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
