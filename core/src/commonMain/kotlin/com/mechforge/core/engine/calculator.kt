package com.mechforge.core.engine

import com.mechforge.core.units.Units
import com.mechforge.core.util.Fmt
import kotlin.math.floor

/**
 * Base class for every calculator. Subclasses implement only the math on
 * base-SI values; spec validation is shared here (README §20/§34).
 */
abstract class Calculator(val def: CalculatorDefinition) {

    fun run(inputs: Map<String, InputValue>): CalcOutput {
        val errors = validateDefinition(def, inputs)
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return calculate(inputs)
    }

    protected abstract fun calculate(inputs: Map<String, InputValue>): CalcOutput

    protected fun value(inputs: Map<String, InputValue>, id: String): Double =
        inputs.getValue(id).baseValue

    protected fun optionalValue(inputs: Map<String, InputValue>, id: String, default: Double): Double =
        inputs[id]?.baseValue ?: default

    protected fun has(inputs: Map<String, InputValue>, id: String): Boolean =
        inputs.containsKey(id)

    protected fun result(
        id: String,
        label: String,
        value: Double,
        unitId: String,
        isPrimary: Boolean = false,
        isRecommended: Boolean = false,
    ) = ResultValue(id, label, value, unitId, isPrimary, isRecommended)

    companion object {
        /** Generic spec-driven validation shared by all calculators. */
        fun validateDefinition(
            def: CalculatorDefinition,
            inputs: Map<String, InputValue>,
        ): List<InputError> {
            val errors = mutableListOf<InputError>()
            for (spec in def.inputs) {
                val iv = inputs[spec.id]
                if (iv == null) {
                    if (spec.required) {
                        errors += InputError(spec.id, "${spec.label} is required.")
                    }
                    continue
                }
                val unit = runCatching { Units.byId(iv.displayUnitId) }.getOrNull()
                if (unit == null || unit.family != spec.family) {
                    errors += InputError(spec.id, "Invalid unit for ${spec.label}.")
                    continue
                }
                spec.minValue?.let { min ->
                    val tooLow = if (spec.exclusiveMin) iv.baseValue <= min else iv.baseValue < min
                    if (tooLow) {
                        val bound = if (spec.exclusiveMin) "greater than" else "at least"
                        errors += InputError(spec.id, "${spec.label} must be $bound ${boundText(min, unit.symbol, unit.id)}.")
                    }
                }
                spec.options?.let { options ->
                    val index = iv.baseValue
                    if (index < 0.0 || index > (options.size - 1).toDouble() || index != floor(index)) {
                        errors += InputError(
                            spec.id,
                            "${spec.label} must be one of the listed options (0..${options.size - 1}).",
                        )
                    }
                }
                spec.maxValue?.let { max ->
                    val tooHigh = if (spec.exclusiveMax) iv.baseValue >= max else iv.baseValue > max
                    if (tooHigh) {
                        val bound = if (spec.exclusiveMax) "less than" else "at most"
                        errors += InputError(spec.id, "${spec.label} must be $bound ${boundText(max, unit.symbol, unit.id)}.")
                    }
                }
            }
            return errors
        }

        private fun boundText(baseBound: Double, symbol: String, unitId: String): String {
            val display = runCatching { Units.byId(unitId).fromBase(baseBound) }.getOrElse { baseBound }
            return "${Fmt.n(display, 4)} $symbol"
        }
    }
}
