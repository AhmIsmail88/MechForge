<div align="center">

<img src="docs/assets/cover.png" width="100%" alt="MechForge - 62 verified mechanical engineering calculators, offline-first, Arabic and English, PDF and Excel reports">

**A local-first mechanical-engineering toolkit for engineers who need to check their numbers.**

Hydraulics · HVAC · Thermodynamics · Mechanical design · Piping · Water &amp; wastewater · Fire protection · Equipment

![version](https://img.shields.io/badge/version-0.2.0-1856FF?style=flat-square)
![platform](https://img.shields.io/badge/platform-Android%208%2B%20%7C%20Desktop%20JVM-3A344E?style=flat-square)
![kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?style=flat-square)
![compose](https://img.shields.io/badge/Compose%20Multiplatform-1.9.0-4285F4?style=flat-square)
![tests](https://img.shields.io/badge/tests-259%20engine%20%2B%2016%20app-07CA6B?style=flat-square)
![permissions](https://img.shields.io/badge/permissions-none%20(no%20INTERNET)-EA2143?style=flat-square)

<img src="docs/assets/screenshot-desktop.png" width="860" alt="MechForge on the desktop: the glass interface, the calculator library and the reference library">

</div>

---

## Overview

MechForge is a mechanical-engineering calculator suite that runs **entirely on your device**.
It is built around one idea: an engineering number is only useful if you can check it, so
**every calculator shows its formula, its calculation steps and its engineering reference**
next to the result — never just a bare number.

- **62 calculators**, each with declared inputs, units, validation and references.
- **Offline-first**: no account, no telemetry, no network. The Android app declares **no
  INTERNET permission** at all.
- **Arabic and English interface**, with a **report language** that is chosen separately
  from the app language (Arabic reports are laid out right-to-left).
- **Engineering reports**: a paginated **PDF** and a formatted **Excel workbook** with the
  same block order — letterhead, inputs, formula, steps, results, warnings, notes,
  reference and a signature block.
- **One engine, two apps**: the same Kotlin Multiplatform engine and the same Compose UI
  are used by the Android app and the desktop (JVM) app.

## Calculator catalogue

| Category | Calculators | Examples |
| --- | --- | --- |
| Hydraulics | 10 | pump power, pipe velocity, Reynolds number, Darcy-Weisbach, friction factor, orifice flow, Manning, Hazen-Williams, NPSH available, minor losses |
| HVAC | 10 | sensible heat, total cooling load, airflow converter, kW ↔ TR / COP / EER, latent heat, duct velocity, duct sizing, duct friction loss, fan power, air changes per hour |
| Thermodynamics | 6 | ideal gas, Carnot efficiency, thermal efficiency, isentropic relations, compressor power, LMTD |
| Mechanical design | 8 | power ↔ torque ↔ speed, torsional stress, bolt torque and design preload, bearing life (L10 / Lnm), spring rate, simply supported beam, cantilever beam, gear ratio |
| Piping | 6 | pipe sizing, pipe weight (with content), wall thickness (ASME B31.3 form), thermal expansion, equivalent length, valve Kv / Cv |
| Water &amp; wastewater | 5 | tank volume, detention time, chlorine dose, peak flow, hydraulic loading |
| Fire protection | 7 | sprinkler discharge (K-factor), hose / nozzle flow, fire pump head, fire pump power, water hammer (Joukowsky), **FM-200 (HFC-227ea) total flooding**, **CO₂ total flooding (NFPA 12)** |
| Equipment | 5 | heat exchanger duty, effectiveness-NTU, pump affinity laws, fan laws, multi-stage compression ratio |
| Unit conversion | 5 | pressure, flow, power, length, temperature |

The fire-protection and equipment calculators are driven by the **hazard class** rather
than by a concentration the user has to look up: choose the class and the design
concentration, flooding factor, agent quantity and cylinder count follow (CO₂ uses the
standard 45 kg cylinder charge).

## Engineering reports

| | PDF | Excel (.xlsx) |
| --- | --- | --- |
| Layout | paginated A4 sheet with letterhead | one worksheet, styled sections |
| Content | inputs, formula, steps, results, warnings, notes, reference, signatures + "By Ahmed Ismail" | identical block order |
| Company logo | yes (uploaded in Settings) | — |
| Language | Arabic (right-to-left) or English, chosen in Settings | same, including the sheet direction |

Both renderers consume the same structured report model, so the two outputs can never
drift apart.

## Reference library

Engineering data lives next to the calculator, not inside it:

- **Built-in generic datasets** (self-authored values only): typical pipe roughness,
  material densities, water properties, physical constants, IEC motor ratings.
- **Import your own** licensed data as **CSV** (`key,value,unit,notes`) or **JSON**
  (array of rows, or an object with `name`, `category`, `source`, `license` and `rows`).
- Every dataset carries its **source** and **licence**, and a dataset can **fill a
  calculator input** directly (for example the roughness dataset feeding ε).

## Verification

Correctness is treated as a first-class feature, and is checked twice:

1. **Unit tests** in the engine and the app: **259** engine tests (`:core:jvmTest`) plus
   **16** app tests (`:composeApp:desktopTest`) covering the database migrations, the
   reference-library import and the report renderers.
2. **An independent implementation**: `tools/verification/` re-derives the equations in
   Python from a dump of the engine's own output. The current run covers **129
   scenarios**, **433 result comparisons** and **all 62 calculators**, with **0 value
   mismatches** and **0 scaling-law failures**.

```bash
./gradlew :core:jvmTest :composeApp:desktopTest

# independent cross-check
python tools/verification/extract_dump.py core/build/test-results/jvmTest/TEST-com.mechforge.core.EngineOutputDumpTest.xml engine_dump.jsonl
python tools/verification/verify_calculations.py engine_dump.jsonl
```

## Architecture

```
core/         Kotlin Multiplatform engine: units, validation, 62 calculators (no UI or DB dependencies)
composeApp/   Compose Multiplatform app, shared by both platforms
              commonMain/   shared UI, data layer, report model
              androidMain/  Android entry point, manifest, icons, SQLite driver
              desktopMain/  desktop entry point, SQLite driver, native file dialogs
docs/         decision records (ADR style) and image assets
tools/        verification tooling (not shipped with the app)
```

Rules the codebase keeps to:

- Engineering formulas live **only** in `core` (`com.mechforge.core.calcs`); the UI never
  performs engineering math.
- Every calculator declares its formula, reference, assumptions and validation, and takes
  at least three tests (textbook case, unit-conversion case, edge case).
- No copyrighted standard tables are embedded: the tools compute, cite and let you import
  licensed data — they do not certify code compliance.

## Build and run

Requirements: a **JDK 17+** through `JAVA_HOME` (the Android Studio JBR works), and for
the Android target the Android SDK (via `ANDROID_HOME`, or `sdk.dir` in the gitignored
`local.properties`).

```bash
# desktop app
./gradlew :composeApp:run

# Android app on a connected phone
./gradlew :composeApp:installDebug

# tests
./gradlew :core:jvmTest :composeApp:desktopTest

# signed release APK (uses signing/keystore.properties, which is gitignored)
./gradlew :composeApp:assembleRelease
```

On Windows, `run-mechforge.bat` starts the desktop app with the JBR already set.

## Data and privacy

- Desktop database: `~/.mechforge/mechforge.db` (history, favourites, projects, settings,
  the calculator and unit registries).
- On Android it lives in the app's private storage and disappears when the app is
  uninstalled.
- Nothing leaves the device. The Android app requests **no INTERNET permission**; the only
  declared permission is `POST_NOTIFICATIONS`, used solely for the "report exported"
  notification.

## Roadmap

- Arabic content for the calculators themselves (names, descriptions, notes and steps are
  English in this phase; the interface and the reports are already fully bilingual).
- More fire-protection depth (CO₂ pipe/nozzle sizing and venting), and additional
  equipment calculators.
- Desktop packaging (`jpackage`) and a published release with installable artefacts.

## License and credits

- **No license file yet** — all rights reserved by the author; add a licence before any
  redistribution.
- The application embeds **no copyrighted standard tables**; formulas are implemented and
  cited, and licensed data is imported by the user.
- Built by **Ahmed Ismail**.

---

<sub>MechForge 0.2.0 · this file is the project overview; the authoritative product
specification lives in <code>MechForge_README_v2.md</code>.</sub>
