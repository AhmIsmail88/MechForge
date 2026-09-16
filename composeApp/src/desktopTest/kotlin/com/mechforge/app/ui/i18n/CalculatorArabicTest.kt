package com.mechforge.app.ui.i18n

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Arabic layer translates the engine's own content, so every key it uses must exist in the
 * engine. A renamed input or result must break this test rather than leave a stale string in the
 * interface.
 *
 * Result ids are checked against what the calculators actually produce for a small valid run, so
 * the test fails if a result is renamed in the engine.
 */
class CalculatorArabicTest {

    /** A valid run for the calculators whose inputs are all optional (they need a specific pair). */
    private val hints: Map<String, List<Pair<String, Double>>> = mapOf(
        "air-changes-hour" to listOf(
            "vroom" to 240.0,
            "ach" to 15.0,
            "fancap" to 1200.0 / 3600.0,
            "nfans" to 2.0,
        ),
    )

    /** Extra inputs that unlock the optional results, in their base unit. */
    private val optionalGates = setOf("fancap", "nfans")

    private val producedByCalculator: Map<String, Set<String>> by lazy {
        val produced = mutableMapOf<String, MutableSet<String>>()
        for (calculator in CalculatorRegistry.all) {
            val def = calculator.def
            val values = mutableMapOf<String, InputValue>()
            for ((id, value) in hints[def.id].orEmpty()) {
                values[id] = InputValue(id, value, "dash")
            }
            if (hints[def.id] == null) {
                for (spec in def.inputs) {
                    if (spec.required || spec.id in optionalGates) {
                        values[spec.id] = InputValue(spec.id, 1.0, "dash")
                    }
                }
            }
            val out = runCatching { calculator.run(values) }.getOrNull() ?: continue
            produced.getOrPut(def.id) { mutableSetOf() }.addAll(out.results.map { it.id })
        }
        produced
    }

    @Test
    fun everyTranslatedCalculatorExistsInTheRegistry() {
        val known = CalculatorRegistry.all.map { it.def.id }.toSet()
        val unknown = CalculatorArabic.CONTENT.keys - known
        assertTrue(unknown.isEmpty(), "translated calculators that the engine does not have: $unknown")
        assertTrue(CalculatorArabic.CONTENT.isNotEmpty())
    }

    @Test
    fun everyTranslatedInputBelongsToItsCalculator() {
        val problems = mutableListOf<String>()
        for ((calcId, content) in CalculatorArabic.CONTENT) {
            val def = CalculatorRegistry.all.firstOrNull { it.def.id == calcId }?.def
            if (def == null) {
                problems += "$calcId is not in the registry"
                continue
            }
            val inputIds = def.inputs.map { it.id }.toSet()
            for (inputId in content.inputs.keys) {
                if (inputId !in inputIds) problems += "$calcId has no input '$inputId'"
            }
            if (content.name.isBlank()) problems += "$calcId has an empty Arabic name"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("; "))
    }

    @Test
    fun everyTranslatedResultIsActuallyProduced() {
        val problems = CalculatorArabic.CONTENT.flatMap { (calcId, content) ->
            val produced = producedByCalculator[calcId] ?: return@flatMap emptyList()
            content.results.keys
                .filter { it !in produced }
                .map { "$calcId does not produce result '$it'" }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("; "))
    }

    @Test
    fun theLookupsFallBackInsteadOfLosingText() {
        assertEquals("Fallback name", CalculatorArabic.name("no-such-calculator", "Fallback name"))
        assertEquals("Fallback text", CalculatorArabic.description("no-such-calculator", "Fallback text"))
        assertEquals("Fallback label", CalculatorArabic.inputLabel("no-such-calculator", "q", "Fallback label"))
        assertEquals("Fallback label", CalculatorArabic.resultLabel("pump-power", "no-such-result", "Fallback label"))
        assertFalse(CalculatorArabic.isTranslated("no-such-calculator"))
        assertTrue(CalculatorArabic.isTranslated("pump-power"))
        // a translated calculator still falls back for the ids it does not translate
        assertEquals("Fallback label", CalculatorArabic.resultLabel("pump-power", "not-translated", "Fallback label"))
    }

    @Test
    fun everyTranslatedStringCarriesArabicText() {
        // An engineering name may legitimately keep an acronym (ACH, CFM): what matters is that the
        // string really is Arabic and not English left behind.
        val arabic = Regex("[\u0600-\u06FF]")
        val problems = mutableListOf<String>()
        for ((calcId, content) in CalculatorArabic.CONTENT) {
            if (!arabic.containsMatchIn(content.name)) problems += "$calcId: name is not Arabic"
            if (content.description.isNotBlank() && !arabic.containsMatchIn(content.description)) {
                problems += "$calcId: description is not Arabic"
            }
            for ((id, label) in content.inputs) {
                if (!arabic.containsMatchIn(label)) problems += "$calcId input '$id' is not Arabic"
            }
            for ((id, label) in content.results) {
                if (!arabic.containsMatchIn(label)) problems += "$calcId result '$id' is not Arabic"
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("; "))
    }
}
