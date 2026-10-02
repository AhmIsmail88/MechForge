package com.mechforge.core.engine

/**
 * A short, stable fingerprint of the calculation engine.
 *
 * A calculation package has to be traceable to the engine that produced it: the same inputs on a
 * different build can legitimately give a different answer after a correction, and a report that does
 * not say which build it came from cannot be audited later. The fingerprint is the calculator count
 * plus a hash over the sorted calculator ids, so it changes when a calculator is added, removed or
 * renamed - the events that change what the engine can compute.
 *
 * VERSION is maintained by hand and must be bumped with the application version in the same commit.
 */
object EngineFingerprint {

    /** Kept in step with the application version. */
    const val VERSION: String = "0.5.0"

    /** Changes whenever calculation logic is corrected, independently of app packaging. */
    const val CALCULATION_REVISION: String = "2026-09-30.2"

    val calculatorCount: Int get() = CalculatorRegistry.all.size

    /** FNV-1a over the sorted calculator ids, as eight hex digits. */
    val short: String by lazy {
        var hash = 2166136261L
        for (id in CalculatorRegistry.all.map { it.def.id }.sorted()) {
            for (ch in id) {
                hash = (hash xor ch.code.toLong()) and 0xFFFFFFFFL
                hash = (hash * 16777619L) and 0xFFFFFFFFL
            }
            hash = (hash xor 124L) and 0xFFFFFFFFL
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        hash.toString(16).padStart(8, '0')
    }

    /** One line, as printed in a report. */
    val text: String get() = "MechForge " + VERSION + " - " + calculatorCount + " calculators - registry " + short + " - calculations " + CALCULATION_REVISION
}
