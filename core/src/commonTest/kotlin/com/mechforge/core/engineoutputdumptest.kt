package com.mechforge.core

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.test.Test

/**
 * Verification harness: runs every calculator against a fixed scenario table and prints
 * the raw engine output as JSON on stdout.
 *
 * Purpose: make the engine's numbers available to an INDEPENDENT implementation (outside
 * this repository) so the calculations can be cross-checked without sharing code.
 * It asserts nothing by itself — it only dumps values. Extracted from the JUnit
 * system-out of this test.
 */
class EngineOutputDumpTest {

    private fun iv(id: String, value: Double, unitId: String) =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    private fun dump(calcId: String, scenario: String, vararg inputs: InputValue) {
        val calc: Calculator = CalculatorRegistry.byIdOrThrow(calcId)
        val results = buildString {
            append('{')
            var first = true
            for ((k, v) in inputs.associateBy { it.inputId }) {
                if (!first) append(',')
                first = false
                append("\"${k}\":{\"value\":${v.baseValue},\"unit\":\"${v.displayUnitId}\"}")
            }
            append('}')
        }
        val output = runCatching { calc.run(inputs.associateBy { it.inputId }) }
        val outJson = output.getOrNull()?.let { out ->
            out.results.joinToString(",", "{", "}") { r ->
                "\"${r.id}\":${java.lang.Double.toString(r.value)}"
            }
        } ?: "{\"__error__\":true}"
        println("DUMP|{\"calc\":\"$calcId\",\"scenario\":\"$scenario\",\"inputs\":$results,\"results\":$outJson}")
    }

    @Test
    fun dumpAllCalculatorScenarios() {
        // --- Hydraulics ---
        dump("pump-power", "base", iv("q", 100.0, "m3h"), iv("h", 50.0, "m"), iv("eta", 80.0, "pct"), iv("rho", 1000.0, "kgm3"))
        dump("pump-power", "double-flow", iv("q", 200.0, "m3h"), iv("h", 50.0, "m"), iv("eta", 80.0, "pct"), iv("rho", 1000.0, "kgm3"))
        dump("pipe-velocity", "base", iv("q", 100.0, "m3h"), iv("d", 100.0, "mm"))
        dump("pipe-velocity", "double-flow", iv("q", 200.0, "m3h"), iv("d", 100.0, "mm"))
        dump("reynolds-number", "base", iv("v", 1.0, "ms"), iv("d", 50.0, "mm"), iv("nu", 1.0, "cst"))
        dump("reynolds-number", "double-velocity", iv("v", 2.0, "ms"), iv("d", 50.0, "mm"), iv("nu", 1.0, "cst"))
        dump("darcy-weisbach", "fixed-f", iv("v", 2.0, "ms"), iv("d", 100.0, "mm"), iv("l", 100.0, "m"), iv("f", 0.02, "dash"))
        dump("darcy-weisbach", "fixed-f-double-v", iv("v", 4.0, "ms"), iv("d", 100.0, "mm"), iv("l", 100.0, "m"), iv("f", 0.02, "dash"))
        dump("darcy-weisbach", "colebrook-smooth", iv("v", 2.0, "ms"), iv("d", 100.0, "mm"), iv("l", 100.0, "m"), iv("re", 100000.0, "dash"))
        dump("minor-losses", "base", iv("k", 1.0, "dash"), iv("v", 2.0, "ms"))
        dump("minor-losses", "double-velocity", iv("k", 1.0, "dash"), iv("v", 4.0, "ms"))
        dump("friction-factor", "re-1e5", iv("re", 100000.0, "dash"), iv("eps", 0.02, "mm"), iv("d", 100.0, "mm"))
        dump("friction-factor", "re-1e6", iv("re", 1000000.0, "dash"), iv("eps", 0.02, "mm"), iv("d", 100.0, "mm"))
        dump("orifice-flow", "base", iv("cd", 0.62, "dash"), iv("d", 50.0, "mm"), iv("h", 2.0, "m"))
        dump("orifice-flow", "quadruple-head", iv("cd", 0.62, "dash"), iv("d", 50.0, "mm"), iv("h", 8.0, "m"))
        dump("manning", "base", iv("n", 0.013, "dash"), iv("r", 0.075, "m"), iv("s", 0.001, "dash"), iv("a", 0.070686, "m2"))
        dump("manning", "quadruple-slope", iv("n", 0.013, "dash"), iv("r", 0.075, "m"), iv("s", 0.004, "dash"), iv("a", 0.070686, "m2"))
        dump("hazen-williams", "d-300", iv("c", 130.0, "dash"), iv("d", 300.0, "mm"), iv("s", 0.001, "dash"))
        dump("hazen-williams", "d-600", iv("c", 130.0, "dash"), iv("d", 600.0, "mm"), iv("s", 0.001, "dash"))
        dump("npsh-available", "suction-lift", iv("hs", -6.0, "m"), iv("hf", 2.5, "m"))
        dump("npsh-available", "flooded", iv("hs", 3.0, "m"), iv("hf", 1.5, "m"))

        // --- HVAC ---
        dump("sensible-heat", "base", iv("q", 1800.0, "m3h"), iv("tin", 20.0, "c"), iv("tout", 30.0, "c"))
        dump("sensible-heat", "double-flow", iv("q", 3600.0, "m3h"), iv("tin", 20.0, "c"), iv("tout", 30.0, "c"))
        dump("total-cooling-load", "full", iv("q", 8496.0, "m3h"), iv("tin", 24.0, "c"), iv("tout", 13.0, "c"), iv("win", 0.010, "dash"), iv("wout", 0.006, "dash"))
        dump("latent-heat", "base", iv("q", 2.0, "m3s"), iv("win", 0.004, "dash"), iv("wout", 0.009, "dash"))
        dump("airflow-converter", "1000cfm", iv("q", 1000.0, "cfm"))
        dump("power-efficiency-converter", "100tr", iv("p", 100.0, "tr"))
        dump("power-efficiency-converter", "cop-10tr", iv("p", 35.168528, "kw"), iv("pelec", 10.0, "kw"))
        dump("duct-velocity", "base", iv("q", 1.0, "m3s"), iv("w", 400.0, "mm"), iv("h", 300.0, "mm"))
        dump("duct-velocity", "round-d400", iv("q", 1.0, "m3s"), iv("d", 400.0, "mm"))
        dump("duct-sizing", "v6", iv("q", 1.0, "m3s"), iv("v", 6.0, "ms"))
        dump("duct-sizing", "v3", iv("q", 1.0, "m3s"), iv("v", 3.0, "ms"))
        dump("duct-pressure-loss", "round", iv("v", 6.0, "ms"), iv("l", 10.0, "m"), iv("d", 400.0, "mm"))
        dump("duct-pressure-loss", "rect", iv("v", 6.0, "ms"), iv("l", 10.0, "m"), iv("w", 500.0, "mm"), iv("h", 300.0, "mm"))
        dump("fan-power", "base", iv("q", 1.0, "m3s"), iv("dp", 500.0, "pa"), iv("etaf", 65.0, "pct"))
        dump("fan-power", "low-eff", iv("q", 1.0, "m3s"), iv("dp", 500.0, "pa"), iv("etaf", 50.0, "pct"))
        dump("fan-power", "motor-size", iv("q", 1.0, "m3s"), iv("dp", 700.0, "pa"), iv("etaf", 100.0, "pct"))
        dump("air-changes-hour", "fan-240m3-ach15", iv("vroom", 240.0, "m3"), iv("ach", 15.0, "perh"))
        dump("air-changes-hour", "fan-480m3-ach15", iv("vroom", 480.0, "m3"), iv("ach", 15.0, "perh"))
        dump("air-changes-hour", "reverse-2000cfm", iv("q", 2000.0, "cfm"), iv("vroom", 300.0, "m3"))

        // --- Thermodynamics ---
        dump("ideal-gas", "air-20c", iv("p", 101.325, "kpa"), iv("t", 20.0, "c"), iv("m", 28.965, "gmol"))
        dump("ideal-gas", "air-0c", iv("p", 101.325, "kpa"), iv("t", 0.0, "c"), iv("m", 28.965, "gmol"))
        dump("carnot-efficiency", "600-300", iv("th", 600.0, "k"), iv("tc", 300.0, "k"))
        dump("carnot-efficiency", "800-300", iv("th", 800.0, "k"), iv("tc", 300.0, "k"))
        dump("thermal-efficiency", "40pct", iv("w", 800.0, "kj"), iv("q", 2000.0, "kj"))
        dump("thermal-efficiency", "45pct", iv("w", 900.0, "kj"), iv("q", 2000.0, "kj"))
        dump("isentropic-relation", "ratio8", iv("p1", 100.0, "kpa"), iv("p2", 800.0, "kpa"), iv("t1", 300.0, "k"))
        dump("isentropic-relation", "ratio4", iv("p1", 100.0, "kpa"), iv("p2", 400.0, "kpa"), iv("t1", 300.0, "k"))
        dump("compressor-power", "eta80", iv("m", 3600.0, "kgh"), iv("t1", 300.0, "k"), iv("p1", 100.0, "kpa"), iv("p2", 800.0, "kpa"))
        dump("compressor-power", "eta70", iv("m", 3600.0, "kgh"), iv("t1", 300.0, "k"), iv("p1", 100.0, "kpa"), iv("p2", 800.0, "kpa"), iv("eta", 70.0, "pct"))
        dump("lmtd", "counter", iv("thin", 90.0, "c"), iv("thout", 60.0, "c"), iv("tcin", 20.0, "c"), iv("tcout", 45.0, "c"))
        dump("lmtd", "parallel", iv("thin", 90.0, "c"), iv("thout", 60.0, "c"), iv("tcin", 20.0, "c"), iv("tcout", 45.0, "c"), iv("arr", 0.0, "dash"))

        // --- Mechanical design ---
        dump("power-torque-rpm", "10kw-1500", iv("p", 10.0, "kw"), iv("n", 1500.0, "rpm"))
        dump("power-torque-rpm", "20kw-1500", iv("p", 20.0, "kw"), iv("n", 1500.0, "rpm"))
        dump("torsional-stress", "100nm-20mm", iv("t", 100.0, "nm"), iv("d", 20.0, "mm"))
        dump("torsional-stress", "200nm-20mm", iv("t", 200.0, "nm"), iv("d", 20.0, "mm"))
        dump("torsional-stress", "dmin", iv("t", 100.0, "nm"), iv("taual", 40.0, "mpa"))
        dump("bolt-torque", "preload", iv("k", 0.2, "dash"), iv("d", 16.0, "mm"), iv("f", 50.0, "kn"))
        dump("bolt-torque", "torque-reverse", iv("k", 0.2, "dash"), iv("d", 16.0, "mm"), iv("t", 160.0, "nm"))
        dump("bolt-torque", "m12-88-65pct", iv("d", 12.0, "mm"), iv("pitch", 1.75, "mm"), iv("class", 1.0, "dash"), iv("preloadpct", 65.0, "pct"))
        dump("bolt-torque", "m16-109-65pct", iv("d", 16.0, "mm"), iv("pitch", 2.0, "mm"), iv("class", 2.0, "dash"), iv("preloadpct", 65.0, "pct"))
        dump("bolt-torque", "util-35kn-88", iv("d", 12.0, "mm"), iv("pitch", 1.75, "mm"), iv("class", 1.0, "dash"), iv("f", 35.0, "kn"))
        dump("bearing-l10", "c30", iv("c", 30.0, "kn"), iv("p", 5.0, "kn"), iv("n", 1500.0, "rpm"))
        dump("bearing-l10", "c60", iv("c", 60.0, "kn"), iv("p", 5.0, "kn"), iv("n", 1500.0, "rpm"))
        dump("bearing-l10", "rel95", iv("c", 30.0, "kn"), iv("p", 5.0, "kn"), iv("exp", 3.0, "dash"), iv("n", 1500.0, "rpm"), iv("rel", 1.0, "dash"))
        dump("bearing-l10", "aiso-08", iv("c", 30.0, "kn"), iv("p", 5.0, "kn"), iv("exp", 3.0, "dash"), iv("n", 1500.0, "rpm"), iv("aiso", 0.8, "dash"))
        dump("spring-rate", "n10", iv("d", 2.0, "mm"), iv("dm", 20.0, "mm"), iv("n", 10.0, "dash"))
        dump("spring-rate", "n20", iv("d", 2.0, "mm"), iv("dm", 20.0, "mm"), iv("n", 20.0, "dash"))
        dump("beam-ss-udl", "L6", iv("w", 10.0, "knlperm"), iv("l", 6.0, "m"), iv("e", 200000.0, "mpa"), iv("i", 1e8, "mm4"))
        dump("beam-ss-udl", "L12", iv("w", 10.0, "knlperm"), iv("l", 12.0, "m"), iv("e", 200000.0, "mpa"), iv("i", 1e8, "mm4"))
        dump("beam-cantilever-point", "L2", iv("p", 5.0, "kn"), iv("l", 2.0, "m"), iv("e", 200000.0, "mpa"), iv("i", 2e7, "mm4"))
        dump("beam-cantilever-point", "L4", iv("p", 5.0, "kn"), iv("l", 4.0, "m"), iv("e", 200000.0, "mpa"), iv("i", 2e7, "mm4"))
        dump("gear-ratio", "3to1", iv("z1", 20.0, "dash"), iv("z2", 60.0, "dash"), iv("n1", 1500.0, "rpm"), iv("t1", 50.0, "nm"))
        dump("gear-ratio", "4to1", iv("z1", 20.0, "dash"), iv("z2", 80.0, "dash"), iv("n1", 1500.0, "rpm"), iv("t1", 50.0, "nm"))

        // --- Piping ---
        dump("pipe-sizing", "100m3h-2ms", iv("q", 100.0, "m3h"), iv("v", 2.0, "ms"))
        dump("pipe-sizing", "200m3h-2ms", iv("q", 200.0, "m3h"), iv("v", 2.0, "ms"))
        dump("pipe-weight", "6in-sch40", iv("od", 168.3, "mm"), iv("t", 7.11, "mm"))
        dump("pipe-weight", "double-od", iv("od", 336.6, "mm"), iv("t", 7.11, "mm"))
        dump("pipe-weight", "with-water", iv("od", 100.0, "mm"), iv("t", 5.0, "mm"), iv("rhoc", 1000.0, "kgm3"))
        dump("pipe-wall-thickness", "p10bar", iv("p", 10.0, "bar"), iv("d", 300.0, "mm"), iv("sigma", 140.0, "mpa"), iv("ca", 2.0, "mm"))
        dump("pipe-wall-thickness", "p20bar", iv("p", 20.0, "bar"), iv("d", 300.0, "mm"), iv("sigma", 140.0, "mpa"), iv("ca", 2.0, "mm"))
        dump("pipe-wall-thickness", "e85", iv("p", 10.0, "bar"), iv("d", 300.0, "mm"), iv("sigma", 140.0, "mpa"), iv("ca", 2.0, "mm"), iv("e", 0.85, "dash"))
        dump("thermal-expansion", "dt50", iv("alpha", 12.0, "permk"), iv("l", 30.0, "m"), iv("dt", 50.0, "delc"))
        dump("thermal-expansion", "dt100", iv("alpha", 12.0, "permk"), iv("l", 30.0, "m"), iv("dt", 100.0, "delc"))
        dump("equivalent-length", "k2.5", iv("k", 2.5, "dash"), iv("d", 100.0, "mm"), iv("f", 0.02, "dash"))
        dump("equivalent-length", "k5", iv("k", 5.0, "dash"), iv("d", 100.0, "mm"), iv("f", 0.02, "dash"))
        dump("valve-kv", "dp1bar", iv("kv", 10.0, "dash"), iv("dp", 1.0, "bar"), iv("sg", 1.0, "dash"))
        dump("valve-kv", "dp4bar", iv("kv", 10.0, "dash"), iv("dp", 4.0, "bar"), iv("sg", 1.0, "dash"))

        // --- Water & wastewater ---
        dump("tank-volume", "2h", iv("q", 100.0, "m3h"), iv("t", 2.0, "h"))
        dump("tank-volume", "4h", iv("q", 100.0, "m3h"), iv("t", 4.0, "h"))
        dump("detention-time", "500-250", iv("v", 500.0, "m3"), iv("q", 250.0, "m3h"))
        dump("detention-time", "1000-250", iv("v", 1000.0, "m3"), iv("q", 250.0, "m3h"))
        dump("chlorine-dose", "5000-2", iv("q", 5000.0, "m3d"), iv("dose", 2.0, "mgl"))
        dump("chlorine-dose", "5000-4", iv("q", 5000.0, "m3d"), iv("dose", 4.0, "mgl"))
        dump("peak-flow", "pf2.5", iv("q", 200.0, "m3h"), iv("pf", 2.5, "dash"))
        dump("peak-flow", "pf5", iv("q", 200.0, "m3h"), iv("pf", 5.0, "dash"))
        dump("hydraulic-loading", "1000-250", iv("q", 1000.0, "m3d"), iv("a", 250.0, "m2"))
        dump("hydraulic-loading", "1000-500", iv("q", 1000.0, "m3d"), iv("a", 500.0, "m2"))

        // --- Fire protection ---
        dump("sprinkler-discharge", "p-7psi", iv("k", 5.6, "gpmpsi"), iv("p", 7.0, "psi"))
        dump("sprinkler-discharge", "p-28psi", iv("k", 5.6, "gpmpsi"), iv("p", 28.0, "psi"))
        dump("fm200-agent-quantity", "class-a-v100", iv("v", 100.0, "m3"), iv("hazard", 0.0, "dash"), iv("t", 21.0, "c"))
        dump("fm200-agent-quantity", "class-a-v200", iv("v", 200.0, "m3"), iv("hazard", 0.0, "dash"), iv("t", 21.0, "c"))
        dump("fm200-agent-quantity", "class-b-v100", iv("v", 100.0, "m3"), iv("hazard", 1.0, "dash"), iv("t", 21.0, "c"))
        dump("fm200-agent-quantity", "override-7.5-s", iv("v", 100.0, "m3"), iv("hazard", 0.0, "dash"), iv("c", 7.5, "pct"), iv("t", 21.0, "c"), iv("s", 0.1359, "m3perkg"), iv("mcyl", 40.0, "kg"))
        dump("fm200-agent-quantity", "review-classC-47m3", iv("v", 47.04, "m3"), iv("hazard", 2.0, "dash"), iv("t", 21.0, "c"), iv("mcyl", 30.0, "kg"))
        dump("fm200-agent-quantity", "review-addkg", iv("v", 47.04, "m3"), iv("hazard", 2.0, "dash"), iv("t", 21.0, "c"), iv("addkg", 2.5, "kg"))
        dump("fm200-agent-quantity", "review-volume-slip", iv("v", 4704.0, "m3"), iv("vgross", 50.0, "m3"), iv("hazard", 0.0, "dash"), iv("t", 21.0, "c"))
        dump("co2-agent-quantity", "class-a-v100", iv("v", 100.0, "m3"), iv("hazard", 0.0, "dash"), iv("t", 21.0, "c"))
        dump("co2-agent-quantity", "class-a-v250", iv("v", 250.0, "m3"), iv("hazard", 0.0, "dash"), iv("t", 21.0, "c"))
        dump("co2-agent-quantity", "deep-seated-v100", iv("v", 100.0, "m3"), iv("hazard", 3.0, "dash"), iv("t", 21.0, "c"))
        dump("co2-agent-quantity", "override-30-c20kg", iv("v", 3531.4667, "ft3"), iv("hazard", 0.0, "dash"), iv("c", 30.0, "pct"), iv("t", 21.0, "c"), iv("mcyl", 20.0, "kg"))
        dump("co2-agent-quantity", "review-nfpa12-dse", iv("v", 47.04, "m3"), iv("hazard", 4.0, "dash"), iv("t", 21.0, "c"), iv("ftable", 1.60, "kgm3"), iv("mcyl", 45.0, "kg"))
        dump("co2-agent-quantity", "review-openings", iv("v", 47.04, "m3"), iv("hazard", 4.0, "dash"), iv("t", 21.0, "c"), iv("ftable", 1.60, "kgm3"), iv("addkg", 10.0, "kg"))
        dump("hose-nozzle-flow", "base", iv("d", 1.5, "in"), iv("p", 100.0, "psi"))
        dump("hose-nozzle-flow", "c-0.98", iv("d", 1.5, "in"), iv("p", 100.0, "psi"), iv("c", 0.98, "dash"))
        dump("fire-pump-head", "base", iv("preq", 8.0, "bar"), iv("pavail", 2.0, "bar"), iv("hstatic", 20.0, "m"), iv("hf", 12.0, "m"), iv("rho", 998.2, "kgm3"))
        dump("fire-pump-power", "base", iv("q", 2500.0, "lmin"), iv("h", 93.2935, "m"), iv("eta", 75.0, "pct"), iv("rho", 998.2, "kgm3"))
        dump("water-hammer", "base", iv("rho", 1000.0, "kgm3"), iv("c", 1200.0, "ms"), iv("dv", 2.0, "ms"), iv("l", 1000.0, "m"))
        dump("water-hammer", "computed-c", iv("rho", 1000.0, "kgm3"), iv("dv", 2.0, "ms"), iv("d", 100.0, "mm"), iv("t", 5.0, "mm"))

        // --- Equipment ---
        dump("heat-exchanger-duty", "m-1kgs", iv("m", 1.0, "kgs"), iv("cp", 4.186, "kjkgk"), iv("tin", 30.0, "c"), iv("tout", 50.0, "c"), iv("u", 500.0, "wm2k"), iv("a", 10.0, "m2"), iv("thin", 90.0, "c"), iv("thout", 60.0, "c"), iv("tcin", 20.0, "c"), iv("tcout", 45.0, "c"), iv("arr", 1.0, "dash"))
        dump("heat-exchanger-duty", "m-2kgs", iv("m", 2.0, "kgs"), iv("cp", 4.186, "kjkgk"), iv("tin", 60.0, "c"), iv("tout", 80.0, "c"))
        dump("hx-effectiveness-ntu", "ntu2-cr0", iv("ntu", 2.0, "dash"), iv("cr", 0.0, "dash"))
        dump("hx-effectiveness-ntu", "ntu2-cr1-counter", iv("ntu", 2.0, "dash"), iv("cr", 1.0, "dash"), iv("arr", 1.0, "dash"))
        dump("pump-affinity-laws", "n2-1800", iv("q1", 100.0, "m3h"), iv("h1", 50.0, "m"), iv("p1", 10.0, "kw"), iv("n1", 1500.0, "rpm"), iv("n2", 1800.0, "rpm"))
        dump("pump-affinity-laws", "n2-1200", iv("q1", 100.0, "m3h"), iv("h1", 50.0, "m"), iv("p1", 10.0, "kw"), iv("n1", 1500.0, "rpm"), iv("n2", 1200.0, "rpm"))
        dump("fan-laws", "n2-1200", iv("q1", 10000.0, "m3h"), iv("dp1", 500.0, "pa"), iv("p1", 5.0, "kw"), iv("n1", 1000.0, "rpm"), iv("n2", 1200.0, "rpm"), iv("rho1", 1.2, "kgm3"), iv("rho2", 1.2, "kgm3"))
        dump("fan-laws", "n2-1000", iv("q1", 10000.0, "m3h"), iv("dp1", 500.0, "pa"), iv("p1", 5.0, "kw"), iv("n1", 1000.0, "rpm"), iv("n2", 1000.0, "rpm"), iv("rho1", 1.2, "kgm3"), iv("rho2", 1.2, "kgm3"))
        dump("compression-ratio", "16bar", iv("p1", 1.0, "bar"), iv("p2", 16.0, "bar"), iv("crmax", 4.0, "dash"))
        dump("compression-ratio", "4bar", iv("p1", 1.0, "bar"), iv("p2", 4.0, "bar"), iv("crmax", 4.0, "dash"))

        // --- Unit converters ---
        dump("pressure-converter", "1bar", iv("v", 1.0, "bar"))
        dump("flow-converter", "1m3h", iv("v", 1.0, "m3h"))
        dump("power-converter", "1kw", iv("v", 1.0, "kw"))
        dump("length-converter", "1m", iv("v", 1.0, "m"))
        dump("temperature-converter", "100c", iv("v", 100.0, "c"))
    }
}
