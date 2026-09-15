package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.math.FrictionFactor
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.abs

/**
 * Darcy-Weisbach head loss: h_f = f·(L/D)·v²/(2g).
 * The friction factor comes either from the user (f) or from Reynolds + roughness
 * via Colebrook-White.
 */

private val Def = CalculatorDefinition(
    id = "darcy-weisbach",
    name = "Darcy-Weisbach Head Loss",
    category = CalculatorCategory.HYDRAULICS,
    description = "Friction head loss in a pipe. Friction factor from Colebrook-White (Re + roughness) or entered directly.",
    formulaDisplay = "h_f = f·(L/D)·v²/(2g)",
    reference = "Darcy-Weisbach (1857); Colebrook-White (1939); Moody chart equivalent.",
    notes = "g = 9.80665 m/s². In laminar flow f = 64/Re regardless of roughness.",
    keywords = listOf("head loss", "friction", "darcy", "weisbach", "colebrook", "pressure drop"),
    inputs = listOf(
        InputSpec("v", "Velocity", "v", UnitFamily.VELOCITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ms"),
        InputSpec("d", "Internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("l", "Pipe length", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("re", "Reynolds number", "Re", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
        InputSpec("eps", "Absolute roughness", "ε", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "mm", libraryKey = "roughness"),
        InputSpec("f", "Friction factor", "f", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash"),
    ),
)

object DarcyWeisbachCalculator : Calculator(Def) {

    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val v = value(inputs, "v")
        val d = value(inputs, "d")
        val length = value(inputs, "l")
        val hasRe = has(inputs, "re")
        val hasEps = has(inputs, "eps")
        val hasF = has(inputs, "f")

        if (!hasF && !hasRe) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "re", "Provide either Reynolds number (with roughness) or a friction factor f."
                    )
                )
            )
        }
        if (hasF && hasRe) {
            throw com.mechforge.core.engine.ValidationException(
                listOf(
                    com.mechforge.core.engine.InputError(
                        "f", "Enter EITHER a friction factor OR Reynolds number + roughness — not both."
                    )
                )
            )
        }

        val warnings = mutableListOf<String>()
        val f: Double
        var re: Double? = null
        var relRough = 0.0
        if (hasF) {
            f = value(inputs, "f")
            re = null
            if (f <= 0.0) {
                throw com.mechforge.core.engine.ValidationException(
                    listOf(com.mechforge.core.engine.InputError("f", "Friction factor must be positive."))
                )
            }
        } else {
            val reVal = value(inputs, "re")
            relRough = if (hasEps) value(inputs, "eps") / d else 0.0
            f = FrictionFactor.darcy(reVal, relRough)
            re = reVal
            FrictionFactor.regimeWarning(reVal)?.let { warnings += it }
            if (!hasEps) warnings += "Roughness not provided — treated as a hydraulically smooth pipe."
        }

        val hf = f * (length / d) * v * v / (2.0 * G)

        return CalcOutput(
            results = listOf(
                result("hf", "Head Loss", hf, "m", isPrimary = true),
                result("f", "Friction Factor f", f, "dash", isPrimary = false),
            ),
            steps = buildList {
                if (re != null) {
                    add("Friction factor: Colebrook-White at Re = ${Fmt.n(re, 0)} (ε/D = ${Fmt.n(relRough, 6)}) → f = ${Fmt.n(f, 5)}")
                } else {
                    add("Using user-supplied friction factor f = ${Fmt.n(f, 5)}")
                }
                add(
                    "Head loss: h_f = f·(L/D)·v²/(2g) = ${Fmt.n(f, 5)} × (${Fmt.n(length, 2)}/${Fmt.n(d, 4)}) × " +
                        "${Fmt.n(v, 3)}²/(2 × 9.80665) = ${Fmt.n(hf, 3)} m"
                )
            },
            warnings = warnings,
        )
    }

}
