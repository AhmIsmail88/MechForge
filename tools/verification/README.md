# MechForge — independent calculation verification

This folder contains the tooling used to verify the engineering calculations with an
implementation that is **completely separate from the Kotlin engine** (different
language, written directly from the textbook equations).

## What it checks

`verify_calculations.py` re-derives every calculator result in Python and compares it
against the numbers produced by the Kotlin engine, then verifies physical scaling laws
between scenario pairs (e.g. `hf ∝ v²`, `δ ∝ L⁴`, `L10 ∝ (C/P)³`, `Q ∝ √ΔP`).

Because the expected values are computed **in the unit each result declares**, an
incorrect result unit (for example a value in joules labelled kJ) also fails the check.

## How to run it

```bash
# 1. from the repository root (Windows PowerShell: set JAVA_HOME to your JDK first)
./gradlew :core:jvmTest --tests "*EngineOutputDumpTest*"

# 2. extract the dumped numbers from the test report
python tools/verification/extract_dump.py \
  core/build/test-results/jvmTest/TEST-com.mechforge.core.EngineOutputDumpTest.xml \
  engine_dump.jsonl

# 3. compare against the independent implementation
python tools/verification/verify_calculations.py engine_dump.jsonl
```

Expected output ends with:

```
OK: every engine value agrees with the independent Python implementation.
OK: every physical scaling law holds.
```

Both scripts use only the Python standard library.

## Scope and limits

- The dump test asserts nothing: it copies the engine's raw results for fixed inputs.
- Tolerance is 1e-6 relative for closed-form relations and 1e-4 for the iterative
  Colebrook-White solutions (both implementations iterate to ~1e-12, so real
  disagreements are far larger than the tolerance).
- External anchors verified against published sources: Hazen-Williams SI coefficient
  (0.278), Colebrook/Moody values (ε/D = 0.001 at Re = 1e5 → 0.0222; ε/D = 1e-4 at
  Re = 1e6 → 0.0135), laminar `f = 64/Re`, and 6" schedule-40 pipe weight
  (28.26 kg/m published vs 28.27 kg/m computed here — the industry shortcut constant
  0.02466 vs π·ρ/10⁶).
- This is a numerical cross-check of the implemented formulas. It does not replace
  engineering review of assumptions, applicability ranges, or code compliance.
