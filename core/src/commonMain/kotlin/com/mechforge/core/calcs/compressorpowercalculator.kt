package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.pow

/** Isentropic compressor: discharge temperature and shaft power. */
private val Def = CalculatorDefinition(
    id = "compressor-power",
    name = "Compressor Power & Discharge Temperature",
    category = CalculatorCategory.THERMODYNAMICS,
    description = "Isentropic compressor calculation: discharge temperature, ideal work and actual shaft power from the isentropic efficiency.",
    formulaDisplay = "T₂s = T₁·(P₂/P₁)^((k−1)/k) ;  T₂a = T₁ + (T₂s−T₁)/η_is ;  P = ṁ·c_p·(T₂a−T₁)",
    reference = "Ideal-gas compressor relations (e.g. Moran & Shapiro; GPSA for practice values).",
    notes = "c_p is treated as constant. Real-gas behaviour, cooling, and mechanical losses are not included.",
    keywords = listOf("compressor", "power", "discharge temperature", "isentropic", "gas"),
    inputs = listOf(
        InputSpec("m", "Mass flow", "ṁ", UnitFamily.MASS_FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgh"),
        InputSpec("t1", "Inlet temperature", "T₁", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c"),
        InputSpec("p1", "Inlet pressure", "P₁", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("p2", "Discharge pressure", "P₂", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kpa"),
        InputSpec("cp", "Specific heat c_p", "c_p", UnitFamily.SPECIFIC_HEAT, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kjkgk", assumedWhenOmitted = "Gas specific heat assumed as 1005 J/(kg.K) (air at 20 C) - use the value for the gas actually handled."),
        InputSpec("k", "Specific heat ratio", "k", UnitFamily.DIMENSIONLESS, required = false, minValue = 1.0, exclusiveMin = true, defaultUnitId = "dash", assumedWhenOmitted = "Isentropic exponent assumed as 1.4 (air) - use the value for the gas actually handled."),
        InputSpec("eta", "Isentropic efficiency", "η_is", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, defaultUnitId = "pct", assumedWhenOmitted = "Overall efficiency assumed as 0.80 - use the manufacturer figure for the selected machine."),
    ),
)

object CompressorPowerCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val m = value(inputs, "m")
        val t1 = value(inputs, "t1")
        val p1 = value(inputs, "p1")
        val p2 = value(inputs, "p2")
        val cp = optionalValue(inputs, "cp", 1005.0)
        val k = optionalValue(inputs, "k", 1.4)
        val eta = optionalValue(inputs, "eta", 0.8)

        if (p2 <= p1) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("p2", "Discharge pressure must be higher than the inlet pressure."))
            )
        }

        val ratio = p2 / p1
        val t2s = t1 * ratio.pow((k - 1.0) / k)
        val t2a = t1 + (t2s - t1) / eta
        val idealPower = m * cp * (t2s - t1)
        val shaftPower = m * cp * (t2a - t1)

        val stepsAr = listOf(
            "نسبة الضغط: P₂/P₁ = ${Fmt.n(ratio, 3)}",
            "التصريف الأيزنتروبي: T₂s = T₁·(P₂/P₁)^((k−1)/k) = ${Fmt.n(t1, 2)} × ${Fmt.n(ratio, 3)}^${Fmt.n((k - 1.0) / k, 4)} = ${Fmt.n(t2s, 2)} K",
            "التصريف الفعلي: T₂a = T₁ + (T₂s − T₁)/η_is = ${Fmt.n(t1, 2)} + (${Fmt.n(t2s, 2)} − ${Fmt.n(t1, 2)})/${Fmt.n(eta, 3)} = ${Fmt.n(t2a, 2)} K (${Fmt.n(t2a - 273.15, 1)} °C)",
            "قدرة العمود: P = ṁ·c_p·(T₂a − T₁) = ${Fmt.n(m, 4)} × ${Fmt.n(cp / 1000.0, 3)} kJ/(kg·K) × (${Fmt.n(t2a, 2)} − ${Fmt.n(t1, 2)}) K = ${Fmt.n(shaftPower / 1000.0, 2)} kW",
        )

        return CalcOutput(
            stepsAr = stepsAr,
            results = listOf(
                result("t2s", "Isentropic Discharge Temperature", t2s, "k"),
                result("t2a", "Actual Discharge Temperature", t2a, "k", isPrimary = true),
                result("t2ac", "Actual Discharge Temperature (°C)", t2a - 273.15, "c"),
                result("pideal", "Ideal (Isentropic) Power", idealPower / 1000.0, "kw"),
                result("pshaft", "Shaft Power", shaftPower / 1000.0, "kw", isPrimary = true),
                result("ratio", "Pressure Ratio", ratio, "dash"),
            ),
            steps = listOf(
                "Pressure ratio: P₂/P₁ = ${Fmt.n(ratio, 3)}",
                "Isentropic discharge: T₂s = T₁·(P₂/P₁)^((k−1)/k) = ${Fmt.n(t1, 2)} × ${Fmt.n(ratio, 3)}^${Fmt.n((k - 1.0) / k, 4)} = ${Fmt.n(t2s, 2)} K",
                "Actual discharge: T₂a = T₁ + (T₂s − T₁)/η_is = ${Fmt.n(t1, 2)} + (${Fmt.n(t2s, 2)} − ${Fmt.n(t1, 2)})/${Fmt.n(eta, 3)} = ${Fmt.n(t2a, 2)} K (${Fmt.n(t2a - 273.15, 1)} °C)",
                "Shaft power: P = ṁ·c_p·(T₂a − T₁) = ${Fmt.n(m, 4)} × ${Fmt.n(cp / 1000.0, 3)} kJ/(kg·K) × (${Fmt.n(t2a, 2)} − ${Fmt.n(t1, 2)}) K = ${Fmt.n(shaftPower / 1000.0, 2)} kW",
            ),
            warnings = buildList {
                if (!has(inputs, "eta")) add("Isentropic efficiency not provided — assumed 0.80.")
                if (!has(inputs, "cp")) add("c_p not provided — assumed 1.005 kJ/(kg·K) (air).")
                if (t2a > 473.15) add("Discharge temperature above 200 °C — check material limits and consider intercooling.")
            },
            warningsAr = buildList {
                if (!has(inputs, "eta")) add("لم تُدخل الكفاءة الأيزنتروبية - افتُرضت 0.80.")
                if (!has(inputs, "cp")) add("لم تُدخل c_p - افتُرضت 1.005 kJ/(kg·K) (هواء).")
                if (t2a > 473.15) add("حرارة التصريف أعلى من 200 °C - راجع حدود المواد وفكّر في تبريد بيني.")
            },
        )
    }
}
