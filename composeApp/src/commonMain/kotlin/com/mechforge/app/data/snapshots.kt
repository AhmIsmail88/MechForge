package com.mechforge.app.data

import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Persisted input snapshot (JSON) for history / saved calculations. */
@Serializable
data class InputSnapshot(val inputId: String, val value: Double, val unitId: String)

object Snapshots {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), InputSnapshot.serializer())

    fun encodeInputs(inputs: Map<String, InputValue>): String =
        json.encodeToString(
            serializer,
            inputs.mapValues { (_, v) -> InputSnapshot(v.inputId, v.baseValue, v.displayUnitId) },
        )

    fun decodeInputs(jsonText: String): Map<String, InputValue> {
        val raw = json.decodeFromString(serializer, jsonText)
        return raw.mapValues { (_, s) ->
            // Guard against unknown units persisted by newer/older versions.
            val unit = runCatching { Units.byId(s.unitId) }.getOrNull()
                ?: Units.defaultUnit(com.mechforge.core.units.UnitFamily.DIMENSIONLESS)
            InputValue(s.inputId, s.value, unit.id)
        }
    }

    /** Replays a saved result set, or null when the record predates the current model. */
    fun decodeResults(jsonText: String): com.mechforge.core.engine.CalcOutput? =
        runCatching {
            json.decodeFromString(com.mechforge.core.engine.CalcOutput.serializer(), jsonText)
        }.getOrNull()

    fun encodeResults(output: com.mechforge.core.engine.CalcOutput): String =
        json.encodeToString(
            com.mechforge.core.engine.CalcOutput.serializer(),
            output,
        )
}
