package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/** Compression ratio and the number of identical stages. */
private val Def = CalculatorDefinition(
    id = "compression-ratio",
    name = "Compression Ratio (Multi-Stage)",
    category = CalculatorCategory.EQUIPMENT,
    description = "Overall compression ratio and the number of identical stages required when the per-stage ratio is limited.",
    formulaDisplay = "CR = P2/P1 ;  n = ceil(ln(CR)/ln(CR_max)) ;  CR_stage = CR^(1/n)",
    reference = "Compressor staging practice (e.g. GPSA Engineering Data Book); equal per-stage ratios with intercooling back to the inlet temperature.",
    notes = "Assumes each stage has the same ratio and that intercooling returns the gas to the inlet temperature (the usual basis for minimum work). Discharge temperatures must still be checked against the material limits.",
    keywords = listOf("compressor", "compression ratio", "staging", "intercooler", "equipment", "gas"),
    inputs = listOf(
        InputSpec("p1", "Inlet pressure", "P1", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("p2", "Discharge pressure", "P2", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("crmax", "Maximum per-stage ratio", "CR_max", UnitFamily.DIMENSIONLESS, required = false, minValue = 1.0, exclusiveMin = false, defaultUnitId = "dash"),
    ),
)

object CompressionRatioCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val p1 = value(inputs, "p1")
        val p2 = value(inputs, "p2")
        val crMax = optionalValue(inputs, "crmax", 4.0)

        if (p2 < p1) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(com.mechforge.core.engine.InputError("p2", "Discharge pressure must be greater than or equal to the inlet pressure."))
            )
        }

        val ratio = p2 / p1
        val stages = if (ratio <= crMax) 1 else ceil(ln(ratio) / ln(crMax)).toInt()
        val perStage = ratio.pow(1.0 / stages)
        val interstage = p1 * perStage

        return CalcOutput(
            results = listOf(
                result("cr", "Overall Compression Ratio", ratio, "dash", isPrimary = true),
                result("n", "Stages Required", stages.toDouble(), "dash", isPrimary = true),
                result("crStage", "Per-Stage Ratio", perStage, "dash", isPrimary = true),
                result("pint", "First Interstage Pressure", interstage / 1e5, "bar"),
            ),
            steps = listOf(
                "Overall ratio: CR = P2/P1 = ${Fmt.n(p2 / 1e5, 3)} / ${Fmt.n(p1 / 1e5, 3)} = ${Fmt.n(ratio, 3)}",
                "Maximum per-stage ratio: ${Fmt.n(crMax, 2)}",
                if (stages == 1) {
                    "CR <= CR_max - a single stage is sufficient."
                } else {
                    "Stages: n = ceil(ln(${Fmt.n(ratio, 3)})/ln(${Fmt.n(crMax, 2)})) = ${Fmt.n(ln(ratio) / ln(crMax), 3)} -> $stages stages"
                },
                "Equal per-stage ratio: CR_stage = CR^(1/n) = ${Fmt.n(ratio, 3)}^(1/$stages) = ${Fmt.n(perStage, 3)}",
                "First interstage pressure (after intercooling): ${Fmt.n(interstage / 1e5, 3)} bar",
            ),
            warnings = buildList {
                if (!has(inputs, "crmax")) add("Maximum per-stage ratio not provided - assumed 4.0 (typical air compressors; verify with the machine data).")
                if (stages > 4) add("More than 4 stages: consider a different machine type or review the required discharge pressure.")
                add("Check the discharge temperature and the intercooler duty for each stage before selecting the machine.")
            },
        )
    }
}
