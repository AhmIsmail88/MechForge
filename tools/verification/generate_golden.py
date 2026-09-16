#!/usr/bin/env python3
"""Freeze the independent expectations into a Kotlin golden table (audit P0-3).

Reads the engine dump (the scenario table with every input in its SI base value) and
re-derives each expected result with the independent Python models in
verify_calculations.py. The generated Kotlin file therefore contains NO engine output:
the golden numbers come from a separate implementation of the textbook equations, so
GoldenEngineeringTest compares the engine against an independent authority.

Usage:
    ./gradlew :core:jvmTest --tests "*EngineOutputDumpTest*"
    python tools/verification/extract_dump.py \
        core/build/test-results/jvmTest/TEST-com.mechforge.core.EngineOutputDumpTest.xml \
        tools/verification/engine_dump.jsonl
    python tools/verification/generate_golden.py
"""
import io
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from verify_calculations import expectation  # noqa: E402

# results that come from an iterative solution (Colebrook): compare a little looser
LOOSE = {
    ("friction-factor", "f"),
    ("darcy-weisbach", "f"),
    ("duct-pressure-loss", "f"),
    ("duct-pressure-loss", "dp"),
    ("duct-pressure-loss", "dpPerM"),
}
LOOSE_TOL = "1e-4"
STRICT_TOL = "1e-6"


def lit(value):
    value = float(value)
    if value != value or value in (float("inf"), float("-inf")):
        return None
    return repr(value)


def main():
    dump = os.path.join(HERE, "engine_dump.jsonl")
    out = os.path.normpath(os.path.join(
        HERE, "..", "..", "core", "src", "commonTest", "kotlin",
        "com", "mechforge", "core", "GoldenCases.kt"))
    cases, skipped = [], []
    with io.open(dump, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            rec = json.loads(line)
            calc, scenario = rec["calc"], rec["scenario"]
            if rec["results"].get("__error__"):
                skipped.append((calc, scenario, "engine error in the dump"))
                continue
            raw = {k: v["value"] for k, v in rec["inputs"].items()}
            units = {k: v["unit"] for k, v in rec["inputs"].items()}
            if any(lit(v) is None for v in raw.values()):
                skipped.append((calc, scenario, "non-finite input"))
                continue
            try:
                exp = expectation(calc, scenario, raw)
            except Exception as exc:
                skipped.append((calc, scenario, "no independent model: %s" % exc))
                continue
            exp = {k: float(v) for k, v in exp.items()
                   if k in rec["results"] and lit(v) is not None}
            if not exp:
                skipped.append((calc, scenario, "no overlapping result ids"))
                continue
            cases.append((calc, scenario, raw, units, exp))

    head = [
        "package com.mechforge.core",
        "",
        "// GENERATED FILE - do not edit by hand.",
        "//",
        "// Source: tools/verification/generate_golden.py",
        "//",
        "// Every expected value below is re-derived in Python from the textbook equations",
        "// (tools/verification/verify_calculations.py) and frozen here. No value was taken",
        "// from the Kotlin engine, so GoldenEngineeringTest compares the engine against an",
        "// independent implementation rather than against itself.",
        "//",
        "// Regenerate after any intentional engine change:",
        '//   ./gradlew :core:jvmTest --tests "*EngineOutputDumpTest*"',
        "//   python tools/verification/extract_dump.py "
        "core/build/test-results/jvmTest/TEST-com.mechforge.core.EngineOutputDumpTest.xml "
        "tools/verification/engine_dump.jsonl",
        "//   python tools/verification/generate_golden.py",
        "",
        "val GOLDEN_CASES: List<GoldenCase> = listOf(",
    ]
    body = []
    for calc, scenario, raw, units, exp in cases:
        # Kotlin string literals need double quotes, so emit JSON strings, not repr()
        ins = ", ".join("GoldenInput(%s, %s, %s)" % (json.dumps(k), lit(raw[k]), json.dumps(units[k]))
                        for k in sorted(raw))
        outs = ", ".join("%s to %s" % (json.dumps(k), lit(exp[k])) for k in sorted(exp))
        tol = LOOSE_TOL if any((calc, k) in LOOSE for k in exp) else STRICT_TOL
        body += [
            "    GoldenCase(",
            "        calculatorId = %s," % json.dumps(calc),
            "        scenario = %s," % json.dumps(scenario),
            "        inputs = listOf(%s)," % ins,
            "        expected = mapOf(%s)," % outs,
            "        relativeTolerance = %s," % tol,
            "    ),",
        ]
    io.open(out, "w", encoding="utf-8", newline="\n").write(
        "\n".join(head + body + [")", ""]))

    print("cases: %d | inputs: %d | expected values: %d | calculators: %d" % (
        len(cases), sum(len(c[2]) for c in cases), sum(len(c[4]) for c in cases),
        len({c[0] for c in cases})))
    for calc, scenario, why in skipped:
        print("   skipped %s/%s: %s" % (calc, scenario, why))
    return 0


if __name__ == "__main__":
    sys.exit(main())