package com.mechforge.app.export

import com.mechforge.app.data.Snapshots
import com.mechforge.core.engine.*
import kotlin.test.*

class SnapshotRevisionTest {
    @Test fun currentCalculationRoundTripsWithRevision() {
        val out = CalculatorRegistry.byIdOrThrow("pipe-velocity").run(mapOf(
            "q" to InputValue("q", 0.01, "m3s"), "d" to InputValue("d", 0.1, "m")))
        assertEquals(EngineFingerprint.CALCULATION_REVISION, out.calculationRevision)
        assertEquals(out, Snapshots.decodeResults(Snapshots.encodeResults(out)))
    }

    @Test fun oldOrUnstampedResultsCannotReplayAsCurrent() {
        assertNull(Snapshots.decodeResults("{\"results\":[],\"steps\":[],\"warnings\":[]}"))
        val old = CalcOutput(calculationRevision = "2026-09-30.1")
        assertNull(Snapshots.decodeResults(Snapshots.encodeResults(old)))
        assertNull(Snapshots.decodeResults("invalid JSON"))
    }
}
