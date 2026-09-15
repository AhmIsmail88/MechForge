# MechForge — developer quickstart

> Product specification: see **[MechForge_README_v2.md](MechForge_README_v2.md)** (authoritative) and `MechForge_README.md` (v1, kept for history).

MechForge is a local-first mechanical-engineering toolkit. This repository contains the
**Desktop (JVM) app** and an **Android app**, both built from the same shared engine and
the same Compose UI: 62 verified calculators, SQLite persistence via SQLDelight, history,
favorites, projects, converter, search, a reference library, and engineering report
export (PDF + Excel).

**Current version: 0.2.0** (`versionName` in `composeApp/build.gradle.kts`).

## What is in 0.2.0

- **62 calculators** across hydraulics, HVAC, thermodynamics, mechanical design, piping,
  water & wastewater, fire protection, equipment and unit conversion.
- **Arabic and English interface**, plus an independent **report language** setting
  (follow the app language / Arabic / English) that also drives the right-to-left layout
  of an exported report.
- **Engineering reports**: a paginated **PDF** and a formatted **Excel workbook (.xlsx)**
  with the same block order - letterhead, inputs, formula, calculation steps, results,
  warnings, notes, reference and a signature block (with an optional company logo on the
  PDF).
- **Reference library**: built-in generic datasets with source and licence metadata, plus
  CSV/JSON import; a dataset can fill a calculator input directly.
- **Glass interface**: frosted panes over a deep engineering backdrop, light and dark.
- **Verification**: 259 engine tests + 16 app tests, and an independent Python
  re-implementation that cross-checks the engine output (see below).

## Requirements

- JDK 17 or newer, supplied through the `JAVA_HOME` environment variable (the Android
  Studio bundled JBR at `C:\Program Files\Android\Android Studio\jbr` works)
- Windows / macOS / Linux (the desktop target is JVM)

No internet connection is required by the application itself; only the first Gradle
build downloads dependencies.

## Run the app

**Windows — easiest:** double-click **`run-mechforge.bat`** in this folder.

**Any OS, from a terminal:**

```bash
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # or any JDK 17+
.\gradlew.bat :composeApp:run
```

```bash
# bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :composeApp:run
```

The window **“MechForge — Mechanical Engineering Toolkit”** opens with the calculator
library, global converter, search, favorites, history, projects and settings.

## Run it on an Android phone

Requirements: the Android SDK (path in `local.properties`), USB debugging enabled on the
phone (`Settings ▸ About phone ▸ tap “Build number” 7×`, then `Developer options ▸ USB
debugging`), and the phone connected with the USB mode set to *File transfer*.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# check the device is visible (adb lives in $ANDROID_HOME/platform-tools)
adb devices

# build and install the debug app
.\gradlew.bat :composeApp:installDebug
```

Or open the project in Android Studio, select the `composeApp` run configuration with the
phone as target, and press Run. The APK is also written to
`composeApp/build/outputs/apk/debug/composeApp-debug.apk` if you prefer
`adb install -r <apk>`.

App details: package `com.mechforge.app`, minSdk **26** (Android 8.0+), targetSdk 36,
label **MechForge**, **no INTERNET permission** (fully offline). On the phone the database
lives in the app's private storage, so it disappears when the app is uninstalled.

The Android app currently uses the same layout as the desktop app (a mobile-specific UX is
a later phase).

## Where your data lives

- Database: `~/.mechforge/mechforge.db` (`C:\Users\<you>\.mechforge\mechforge.db`)
- It stores calculation history, favorites, projects, settings, the calculator registry
  and the unit registry.
- To start fresh, close the app and delete that file — it is re-created on next launch.
- Nothing is sent anywhere: the app is fully offline.

## Build and test

The build takes its JVM from `JAVA_HOME` — no machine-specific JDK path is pinned in
`gradle.properties`. Point `JAVA_HOME` at any JDK 17+ (the Android Studio JBR works):

```bash
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# bash
export JAVA_HOME=/path/to/jdk-17
```

The Android tasks additionally need the SDK: either via the `ANDROID_HOME` environment
variable, or via `sdk.dir` (forward slashes) in the gitignored `local.properties`.

```bash
./gradlew :core:jvmTest                     # engine unit tests (no Android SDK needed)
./gradlew :composeApp:desktopTest           # database migration test (no Android SDK needed)
./gradlew :composeApp:assembleDebug         # Android debug APK (needs ANDROID_HOME / sdk.dir)
./gradlew :composeApp:createDistributable   # desktop app image (needs jpackage: a full JDK, not a JRE)

# one-shot CI-equivalent run
./gradlew :core:jvmTest :composeApp:desktopTest :composeApp:assembleDebug
```

## Independent calculation verification

The numbers are cross-checked against an independent Python re-implementation of the
same equations — see **[tools/verification/README.md](tools/verification/README.md)**.

## Project layout

```
core/        Kotlin Multiplatform engine: units, validation, 62 calculators (no UI/DB deps)
composeApp/  Compose Multiplatform app:
             commonMain/  shared UI + data layer (used by both platforms)
             androidMain/ Android entry point, manifest, icons, Android SQLite driver
             desktopMain/ desktop entry point, desktop SQLite driver, file-dialog export
docs/        decision records (ADR-style)
tools/       verification tooling (not part of the shipped app)
```

## Architecture rules (from the spec)

- Engineering formulas live **only** in `core` (`com.mechforge.core.calcs`). The UI never
  performs engineering math.
- Every calculator declares its formula, reference, assumptions and validation; result
  screens show input → formula → steps → result.
- No copyrighted standard tables are embedded; the tools compute and cite, they do not
  certify code compliance.
