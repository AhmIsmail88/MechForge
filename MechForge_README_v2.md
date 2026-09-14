# ⚙️ MechForge — Mechanical Engineering Toolkit

> **A professional cross-platform engineering calculation suite for mechanical engineers.**
>
> **Platforms:** Android + Desktop
> **Architecture:** Local-first / Offline-first
> **Database:** Local SQLite (SQLDelight)
> **Stack:** Kotlin Multiplatform + Compose Multiplatform *(see Section 6)*

---

## 1. Project Overview

**MechForge** is a professional engineering application based on the concept of a **Mechanical Engineering Toolkit**.

Its goal is to provide mechanical engineers with one reliable application containing the calculations, converters, engineering references, saved calculations, and future AI-assisted tools they use in daily engineering work.

Target users include engineers working in:

- Mechanical Design
- HVAC / MEP
- Hydraulics
- Pump Stations
- Water & Wastewater
- Piping
- Fire Protection
- Thermodynamics
- Equipment
- Construction
- Technical Office
- Engineering Consultancy
- Operation & Maintenance
- Infrastructure

The application is intentionally designed as a **scalable engineering platform**, not simply a collection of unrelated calculators.

---

# 2. Product Name

## MechForge

### Full descriptive name

**MechForge — Mechanical Engineering Toolkit**

### Meaning

**Mech** = Mechanical Engineering

**Forge** = engineering, building, manufacturing, craftsmanship, strength, and creating practical solutions.

The name is:

- Short
- Professional
- Technical
- Memorable
- Suitable for Android
- Suitable for Windows/Desktop
- Suitable for future Web/API products

### Branding

- **MechForge**
- **MechForge Engineering Toolkit**
- **MechForge Desktop**
- **MechForge Android**

The primary product name should remain **MechForge**.
"Mechanical Engineering Toolkit" is the descriptive subtitle.

---

# 3. Product Vision

MechForge should become the **digital toolbox of the mechanical engineer**.

Instead of searching for individual calculators, the engineer should be able to open one application and quickly find:

- Engineering calculators
- Unit converters
- Engineering formulas
- Reference tables
- Calculation history
- Saved calculations
- Projects
- Engineering notes
- Future AI-assisted engineering workflows

The fundamental workflow is:

```text
Choose Calculation
        ↓
Enter Inputs
        ↓
Validate Inputs
        ↓
Perform Engineering Calculation
        ↓
Show Formula
        ↓
Show Result
        ↓
Save / Copy / Export
```

---

# 4. Core Philosophy

## 4.1 Accuracy First

Engineering correctness is more important than the number of features.

Every calculator must have:

- Verified formula
- Correct units
- Input validation
- Defined assumptions
- Engineering limits where applicable
- Automated test cases
- Reference/source

## 4.2 Transparent Results

The application must not simply display a number.

The user should be able to understand:

**Input → Formula → Calculation → Result**

Example:

```text
PUMP POWER

Q = 100 m³/h
H = 50 m
η = 80%

Hydraulic Power = 13.63 kW
Shaft Power = 17.04 kW

Recommended standard motor ≈ 18.5 kW
```

The formula and assumptions should be available.

## 4.3 Local-First / Offline-First

The core application must work without an internet connection.

The local database is a fundamental component.

Users should be able to:

- Calculate
- Save calculations
- View history
- Use favorites
- Access reference data
- Manage settings

without cloud connectivity.

## 4.4 Pilot Before Scale

Before building 50 calculators, a **small pilot release must prove the full vertical slice**:

> one platform → one calculator → engine → validation → UI → result → history → export

Only repeatable, boring success justifies scaling to the full MVP.

---

# 5. Platforms

## Android

Designed primarily for:

- Site engineers
- Field engineers
- Consultants
- Maintenance engineers
- Engineers needing quick calculations

Priorities:

- Fast startup
- Simple navigation
- Large controls
- Easy unit selection
- Offline operation
- Copy/share results
- Calculation history
- Favorites

## Desktop

Designed primarily for:

- Design engineers
- Technical Office
- Consultants
- Estimation/QS
- Detailed engineering
- Engineering review

Desktop advantages:

- Large workspace
- Multiple calculations
- Larger reference tables
- PDF/Excel reports
- Project organization
- Advanced future tools

---

# 6. Tech Stack & Cross-Platform Strategy

> **Status: DECIDED.** This section records the decision. Alternatives and criteria are kept for the record. Future reversals must be recorded as a Decision Record in `/docs/decisions/`.

## 6.1 Selected Stack

| Layer | Technology |
|---|---|
| Shared business logic | **Kotlin Multiplatform (KMP)** — 100% shared module |
| UI | **Compose Multiplatform** (Android + Desktop JVM) |
| Local database | **SQLite via SQLDelight** (type-safe, schema migrations built-in) |
| Dependency injection | Koin (or hand-rolled constructor injection) |
| Shared testing | `kotlin.test` + KMP test framework (engine tests run on JVM + Android) |
| Desktop packaging | JVM distribution: Windows installer first (msi), macOS/Linux later |
| Build / CI | Gradle Kotlin DSL + GitHub Actions |

## 6.2 Why This Stack

1. **One language for the entire calculation engine.** The engine, unit system, validation, and domain models live in one shared Kotlin module — directly enforcing Section 41 (no formula implemented twice).
2. **SQLDelight gives versioned, migratable SQLite** on both platforms from day one, matching Section 20's database requirements.
3. **Compose Multiplatform is production-ready on Android and Desktop (JVM)**, the exact two targets of this product.
4. **Testability:** engine tests are plain JVM unit tests — fast, no emulator needed for 95% of the test suite.

## 6.3 Alternatives Considered

| Option | Why Not Selected |
|---|---|
| Flutter | Desktop support and typed local DB (Drift) less mature; Dart duplicates nothing from a future KMP backend. Revisit only if KMP desktop blocks. |
| .NET MAUI | Linux support weak; Android quality/tooling behind native/KMP. |
| Two native apps (Kotlin + C#/Electron) | Directly violates the golden rule: formulas would be implemented twice. |

## 6.4 Decision Records

Any future architectural reversal (e.g., adding iOS, switching UI framework) must be documented in `/docs/decisions/` with context, decision, and consequences.

---

# 7. MVP Scope

The MVP target is approximately **40–50 carefully selected high-value calculations** — but this is **v1.0**, not the first thing built.

## 7.1 Pilot Release v0.1 — 12 Calculators, One Platform

**Platform: Desktop (JVM).** The pilot proves the entire architecture end-to-end with a small, fully-verified set:

| # | Calculator | Category |
|---|---|---|
| 1 | Pump Hydraulic Power & Shaft Power | Hydraulics |
| 2 | Pipe Flow Velocity | Hydraulics |
| 3 | Reynolds Number | Hydraulics |
| 4 | Darcy-Weisbach Head Loss | Hydraulics |
| 5 | Sensible Heat | HVAC |
| 6 | Total Cooling Load (sensible + latent) | HVAC |
| 7 | Airflow Unit Converter (CFM ↔ m³/h ↔ L/s) | HVAC |
| 8 | kW ↔ TR / COP / EER | HVAC |
| 9 | Ideal Gas Law / Gas Density | Thermodynamics |
| 10 | Power ↔ Torque ↔ RPM | Mechanical Design |
| 11 | Torsional Shear Stress | Mechanical Design |
| 12 | Pipe Sizing (velocity-based) | Piping |

### Pilot acceptance criteria (all must pass before v1.0 work starts)

1. All 12 calculators have verified formulas, ≥3 reference test cases each, SI + imperial units, and input validation.
2. Full workflow works offline: calculate → view formula → save to history → reopen → export as text/PDF.
3. Database migrations tested (upgrade from schema v1 → v2 with real data).
4. **5 real engineers** use the pilot for 2+ weeks with zero formula-error reports.
5. UI review pass on Desktop (Section 29 standards).

## 7.2 MVP v1.0 — Full Scope

| Category | Target |
|---|---:|
| Hydraulics | 10 |
| HVAC | 10 |
| Mechanical Design | 8 |
| Thermodynamics | 6 |
| Piping | 6 |
| Water & Wastewater | 5 |
| Unit Conversion | 5 |
| **Total** | **~50** |

Quality and verification are more important than quantity. A calculator ships only after passing the 7-point checklist in Section 46.

---

# 8. Hydraulics Module

Initial/future calculations:

- Flow Rate
- Velocity
- Pipe Diameter
- Reynolds Number
- Darcy-Weisbach Head Loss
- Friction Factor
- Minor Losses
- Static Head
- Dynamic Head
- Pump Head
- Hydraulic Power
- Shaft Power
- Pump Efficiency
- NPSH
- Bernoulli Equation
- Orifice Flow
- Nozzle Flow
- Manning Equation
- Water Hammer
- Surge Tank calculations

Future advanced tools:

- Pump Selection Assistant
- System Curve
- Pump Curve Analysis
- Operating Point
- Pump Affinity Laws

---

# 9. HVAC Module

## Cooling / Heating

- Sensible Heat
- Latent Heat
- Total Cooling Load
- Heating Load
- Air-Side Cooling
- Water-Side Cooling
- COP
- EER
- kW/TR
- TR/kW
- Heat Exchanger Duty

## Air Side

- CFM
- L/s
- m³/h
- Air Velocity
- Duct Sizing
- Duct Pressure Loss
- Equivalent Diameter
- Static Pressure
- Fan Power
- ACH
- Fresh Air Requirement
- Ventilation Rate

## Psychrometrics

Advanced future module:

- Dry Bulb Temperature
- Wet Bulb Temperature
- Relative Humidity
- Dew Point
- Humidity Ratio
- Enthalpy
- Specific Volume
- Sensible Heat Ratio
- Mixing Air
- Cooling Coil

---

# 10. Thermodynamics Module

- Ideal Gas Law
- Density
- Specific Volume
- Enthalpy
- Internal Energy
- Entropy
- Isentropic Relations
- Compressor Work
- Turbine Work
- Pump Work
- Boiler Calculations
- Thermal Efficiency
- Carnot Cycle
- Rankine Cycle
- Otto Cycle
- Diesel Cycle
- Brayton Cycle

---

# 11. Mechanical Design Module

## Shafts

- Power ↔ RPM
- Torque
- Torsional Stress
- Shaft Diameter
- Angle of Twist

## Beams

- Bending Stress
- Shear Stress
- Beam Deflection
- Cantilever
- Simply Supported Beam
- Point Load
- UDL

## Bolts

- Tensile Stress
- Shear Stress
- Bolt Preload
- Bolt Torque
- Basic Bolt Sizing

## Bearings

- Dynamic Load
- Static Load
- Bearing Life
- L10 Life

## Springs

- Spring Rate
- Deflection
- Shear Stress
- Stored Energy

## Gears

- Gear Ratio
- Module
- Pitch Diameter
- Speed Ratio
- Torque Ratio

---

# 12. Piping Module

- Pipe Sizing
- Flow Velocity
- Pressure Drop
- Friction Loss
- Equivalent Length
- Pipe Schedule
- Wall Thickness
- Pipe Weight
- Thermal Expansion
- Valve Cv
- Control Valve Sizing
- Insulation Calculations

Future reference databases:

- Carbon Steel
- Stainless Steel
- Copper
- UPVC
- CPVC
- HDPE
- GRP/FRP
- Ductile Iron

---

# 13. Water & Wastewater Module

Particularly important for infrastructure engineering.

## Water

- Chlorine Dose
- Chemical Dosing
- Contact Time
- CT Value
- Tank Volume
- Detention Time
- Hydraulic Loading

## Pump Stations

- Wet Well Volume
- Pump Duty
- Pump Head
- Pump Power
- Pump Cycling
- Retention Time

## Wastewater

- Average Flow
- Peak Flow
- Peaking Factor
- Hydraulic Loading
- Detention Time
- Basin Volume

---

# 14. Fire Protection Module

Planned calculations:

- Fire Flow
- Sprinkler Flow
- K-Factor
- Orifice Flow
- Pressure
- Pipe Velocity
- Pressure Loss
- Fire Pump Head
- Fire Pump Power
- Hazen-Williams

Where applicable, each calculator should identify its engineering reference.

The application must clearly distinguish between a calculation tool and actual code compliance.

---

# 15. Equipment Module

## Pumps

- Pump Head
- Hydraulic Power
- Shaft Power
- Efficiency
- NPSH
- Affinity Laws

## Fans

- Airflow
- Static Pressure
- Fan Power
- Fan Laws

## Compressors

- Compression Ratio
- Power
- Isentropic Efficiency
- Discharge Temperature

## Heat Exchangers

- Heat Duty
- LMTD
- Effectiveness
- NTU
- Heat Transfer Area

---

# 16. Global Unit Converter

The unit converter should be available throughout the application.

## Pressure

- Pa
- kPa
- MPa
- bar
- psi
- atm
- mH₂O
- ftH₂O

## Flow

- m³/s
- m³/h
- L/s
- L/min
- GPM

## Power

- W
- kW
- HP
- TR

## Length

- mm
- cm
- m
- inch
- ft

## Temperature

- °C
- °F
- K

Additional:

- Mass
- Density
- Force
- Torque
- Energy
- Area
- Volume
- Velocity
- Viscosity

---

# 17. Engineering Reference Library

MechForge should include a structured reference library.

Possible content:

- Pipe dimensions
- Pipe schedules
- Material properties
- Water properties
- Air properties
- Steel properties
- HVAC reference values
- Duct dimensions
- Engineering constants
- Conversion factors
- Common formulas

Every reference dataset should have metadata such as:

- Source
- Edition/version
- Date
- Category
- License status *(see Section 18)*
- Notes

---

# 18. Reference Data Sources & Licensing

> Engineering standards (ASME, ASHRAE, NFPA, AWWA, API, ISO, EN, SMACNA) are **copyrighted publications**. This section defines what MechForge may and may not include.

## 18.1 Rules

1. **Formulas are implemented, not copied.** A published formula (e.g., Darcy-Weisbach, Hazen-Williams) is re-implemented from the engine with a citation. Formulas themselves are ideas, not protected expression — but the surrounding text, tables, and charts are protected.
2. **Never redistribute copyrighted tables** (pipe dimension tables from ASME B36.10, psychrometric tables from ASHRAE, etc.) without a license. Instead:
   - Use manufacturer-published dimensional data (many mills publish OD/WT data publicly),
   - Use public-domain or openly licensed datasets (e.g., NIST data, government publications),
   - Or negotiate/secure a dataset license before shipping that reference.
3. **Every reference record carries license metadata:** `source`, `edition`, `license_type` (public domain / open / licensed / linked-only), `date_added`, `verified_by`.
4. **Where content cannot be legally embedded, link to the standard** and show only the calculator with a note: *"Reference data not included — see ASME B36.10-20xx."*
5. Physical properties (water, air, refrigerants) are **computed from published formulations** (e.g., IAPWS-style correlations, ideal-gas relations with documented coefficients) rather than transcribing copyrighted property tables.

## 18.2 Why This Matters

A licensing violation in a reference table can force recall of shipped builds and destroy professional credibility — the exact asset MechForge is built on. When in doubt: implement the formula, cite the standard, exclude the table.

---

# 19. Calculator Architecture

Every calculator must follow a standardized structure.

```text
Calculator
│
├── Metadata
│   ├── Name
│   ├── Category
│   ├── Description
│   └── Reference
│
├── Inputs
│   ├── Parameter
│   ├── Symbol
│   ├── Value
│   ├── Unit
│   └── Validation
│
├── Formula
│
├── Calculation Engine
│
├── Results
│
├── Engineering Notes
│
└── Calculation History
```

This makes adding new calculators predictable and maintainable.

---

# 20. Calculation Engine

The engineering formulas must be separated from the UI.

Preferred architecture:

```text
Presentation Layer
        ↓
Input Validation
        ↓
Application Services
        ↓
Engineering Calculation Engine
        ↓
Domain Models
        ↓
Result
```

The UI must not contain the actual engineering formulas.

Benefits:

- Easier testing
- Better maintenance
- Android/Desktop consistency
- Easier AI integration
- Future Web/API support
- Reduced duplication

**Implementation note (KMP):** the engine is a pure-Kotlin module with zero UI dependencies. It is consumed by Android and Desktop apps as the same artifact.

---

# 21. Local Database — SQLite

## Database: SQLite (via SQLDelight)

The primary application database will be **local SQLite**, accessed through **SQLDelight** for type-safe queries and versioned migrations.

This is a deliberate architectural decision.

The core application should NOT require:

- Firebase
- Railway
- PostgreSQL
- Cloud database
- Internet access

for normal engineering calculations.

### Local database responsibilities

SQLite should store:

- Calculation history
- Saved calculations
- Favorites
- User settings
- Application preferences
- Recently used calculators
- Reference metadata
- User presets
- Projects
- Application metadata

Conceptual database:

```text
mechforge.db

├── calculators
├── calculation_history
├── saved_calculations
├── favorites
├── units
├── engineering_references
├── user_settings
├── projects
└── app_metadata
```

The schema must be versioned and support database migrations. **Migration tests are mandatory** — every schema change ships with an automated v(n) → v(n+1) migration test using a real fixture database (see Section 7.1 acceptance criteria).

---

# 22. Offline Strategy

Core functionality must remain fully usable offline.

Internet connectivity may be introduced later only for optional functions such as:

- Application updates
- Reference updates
- Optional synchronization
- AI services
- Optional cloud backup
- Online licensing

The application should continue functioning normally when the internet is unavailable.

**Licensing rule:** even license verification must degrade gracefully — a licensed user never loses access to core calculations because they are offline.

---

# 23. Calculation History

A completed calculation can be saved automatically or manually.

Example:

```text
Pump Power
13 Sep 2026

Q = 100 m³/h
H = 50 m
Efficiency = 80%

Result = 17.04 kW
```

Actions:

- Open
- Edit
- Duplicate
- Rename
- Delete
- Favorite
- Export

---

# 24. Favorites

Frequently used calculators should be accessible immediately.

Example:

```text
⭐ Pump Power
⭐ Pipe Velocity
⭐ Head Loss
⭐ Duct Sizing
⭐ Cooling Load
⭐ Flow Converter
```

Favorites should appear on the home dashboard.

---

# 25. Global Search

The application should provide fast search across calculators.

Search examples:

```text
pump
head loss
velocity
Reynolds
cooling load
shaft torque
```

Search should match:

- Calculator name
- Engineering terminology
- Keywords
- Category

---

# 26. AI Engineering Assistant — Future

AI is a future layer, not the foundation of the first release.

Example:

> I have a pump delivering 150 m³/h at 60 m head with 78% efficiency. What motor size should I consider?

Workflow:

```text
User
 ↓
AI Intent Detection
 ↓
Identify Calculator
 ↓
Extract Inputs
 ↓
Engineering Calculation Engine
 ↓
Validate Result
 ↓
AI Explanation
 ↓
Final Answer
```

### Critical rule

> **AI must not be the primary numerical calculation engine.**

The deterministic engineering calculation engine performs the actual mathematics.

AI should handle:

- Natural language
- Calculator discovery
- Input extraction
- Result explanation
- Engineering guidance
- Navigation
- Future report generation

**Data rule:** AI features are opt-in, clearly separated from offline core (Section 38), and never send calculation data without explicit user action.

---

# 27. Android UX

The Android application should prioritize field usability.

Requirements:

- Fast launch
- Simple navigation
- Large touch targets
- Clear numeric inputs
- Easy unit selection
- Offline operation
- Dark/light theme
- Copy result
- Share result
- History
- Favorites

Future:

- OCR input
- Voice input
- Camera-assisted data entry
- PDF reports
- QR project transfer

---

# 28. Desktop UX

The desktop application should provide an engineering workspace.

Concept:

```text
┌─────────────────────────────────────────────┐
│ MechForge                                   │
├──────────────┬──────────────────────────────┤
│ Categories   │ Calculator                   │
│              │                              │
│ Hydraulics   │ Inputs                       │
│ HVAC         │ ┌─────────────────────────┐  │
│ Design       │ │ Q = 100 m³/h            │  │
│ Thermo       │ │ H = 50 m                │  │
│ Piping       │ │ η = 80%                 │  │
│              │ └─────────────────────────┘  │
│ Favorites    │                              │
│ History      │ Result                       │
│ References   │ 17.04 kW                     │
└──────────────┴──────────────────────────────┘
```

Future desktop functionality:

- Multiple calculation panels
- Engineering workspaces
- Project management
- PDF reports
- Excel export
- Batch calculations
- Advanced reference tables
- AI assistant

---

# 29. UI / Visual Identity

The application should communicate:

**Engineering + Precision + Technology**

Visual style:

- Professional
- Clean
- Technical
- Modern
- Minimal
- High readability

Support:

- Light mode
- Dark mode
- Responsive layouts
- Consistent typography
- Engineering symbols
- Consistent cards
- Clear result hierarchy

Avoid a generic "calculator app" appearance.

---

# 30. App Icon / Logo

## Icon Concept

The icon should visually combine three ideas:

### 1. Gear

Represents:

- Mechanical engineering
- Machinery
- Mechanical systems

### 2. Stylized M

A geometric **M** should be integrated into the central symbol.

M = **Mechanical / MechForge**

### 3. Precision / Calculation

A subtle technical or mathematical element may be incorporated:

- Equation line
- Technical grid
- Precision mark
- Geometric calculation symbol

The result should be a **modern engineering emblem**, not a literal calculator.

### Visual direction

```text
        ⚙
     ┌─────┐
     │  M  │
     │ ═══ │
     └─────┘
```

This is only a conceptual representation. The final icon should be sophisticated and geometric.

### Icon requirements

- Recognizable at very small sizes
- Strong silhouette
- Minimal detail
- Works at Android launcher sizes
- Works as Windows desktop icon
- Works on light backgrounds
- Works on dark backgrounds
- Works in monochrome
- Suitable for splash screen
- Suitable for GitHub/project branding
- Suitable for future website

Avoid:

- Cartoon gears
- Generic wrench icons
- Generic calculator icons
- Excessive detail
- Excessive gradients
- Text-heavy icon

---

# 31. Brand Direction

Recommended visual language:

**Industrial Engineering + Modern Software**

Potential color direction:

- Deep engineering blue
- Graphite
- White
- Optional electric cyan accent

The final brand palette should be defined in a dedicated design system.

The icon must remain recognizable without depending on color.

---

# 32. Engineering Standards & References

Potential reference families include, where applicable:

- ASME
- ASHRAE
- NFPA
- AWWA
- ISO
- EN
- API
- SMACNA
- Applicable national/local codes
- Manufacturer technical data

The application must distinguish between:

1. Mathematical calculation
2. Engineering practice
3. Code requirement
4. Manufacturer requirement

The application must never claim code compliance merely because a formula exists.

Where a calculation depends on a standard, the applicable reference/edition should be recorded.

---

# 33. Validation & Testing

Every calculator must have automated tests.

Example:

```text
Test: Pump Power

Q = 100 m³/h
H = 50 m
η = 0.80
ρ = 1000 kg/m³

Expected:
≈ 17.0 kW
```

Testing layers:

```text
Unit Tests
    ↓
Calculation Engine Tests
    ↓
Integration Tests
    ↓
UI Tests
    ↓
Smoke Tests
```

Known engineering examples should be used as reference test cases.

**Coverage rule:** no calculator ships with less than 3 reference test cases, including at least one edge case (e.g., low Reynolds transition, extreme temperature) and one known textbook/worked-example comparison.

---

# 34. Error Handling

The application must never silently accept invalid engineering inputs.

Examples:

- Negative diameter
- Zero efficiency
- Efficiency > 100%
- Invalid temperature
- Missing required inputs
- Impossible unit conversion
- Invalid physical parameters

Example message:

> **Invalid Efficiency**
> Efficiency must be greater than 0% and less than or equal to 100%.

---

# 35. Result Presentation

Results should have a strong visual hierarchy.

Example:

```text
──────────────────────────────
PUMP POWER
──────────────────────────────

Hydraulic Power
13.63 kW

Shaft Power
17.04 kW

Recommended Motor
18.5 kW

──────────────────────────────
Formula
P = ρgQH / η
──────────────────────────────

[ Copy ] [ Save ] [ Share ]
```

Users should be able to change result units without manually recalculating.

---

# 36. Export

Export formats:

- PDF *(v1.0, Desktop)*
- Excel *(v1.1, Desktop)*
- CSV
- Plain Text
- Shareable Calculation Card *(Android)*

Engineering PDF reports may contain:

- MechForge logo
- Calculator name
- Date/time
- Project name
- Engineer name
- Inputs
- Formula
- Result
- Engineering notes
- Reference

---

# 37. Project Workspace

Future versions should support:

```text
Project
 ├── Pump calculations
 ├── Pipe calculations
 ├── HVAC calculations
 ├── Equipment calculations
 └── Saved references
```

This will turn MechForge from a calculator suite into a complete engineering productivity workspace.

---

# 38. Data Model Philosophy

Use structured data rather than hard-coded UI definitions.

Example:

```text
Calculator
- id
- name
- category
- description
- formula
- reference
- version
- active
```

Input:

```text
Input
- id
- calculator_id
- name
- symbol
- data_type
- unit_family
- required
- min_value
- max_value
```

This makes adding future calculators easier and safer.

**Reference dataset metadata extension (from Section 18):**

```text
Reference
- id
- title
- category
- source
- edition
- license_type
- license_notes
- date_added
- verified_by
```

---

# 39. Privacy

Because the core architecture is local-first:

- Calculations remain on the user's device by default.
- No cloud account is required for core calculations.
- Calculation data must not be uploaded without explicit user action.
- Optional AI/cloud features must be clearly separated from offline features.

---

# 40. Business Model & Distribution

> **Status: DIRECTION SET — exact pricing to be validated after the pilot (Section 7.1).**

## 40.1 Model

- **Core calculation engine: free and never paywalled.** Consistent with the Accuracy First philosophy — an engineer's trust is the product's core asset.
- **Desktop Pro:** one-time license or low-cost annual subscription unlocking productivity features — PDF/Excel export, projects, batch calculations, advanced reference tables.
- **Android:** free core with optional Pro unlock (shared entitlement with Desktop where store policies allow).
- **AI features (future):** separate paid add-on, clearly opt-in.

## 40.2 Distribution

- **Android:** Google Play.
- **Desktop:** Microsoft Store **and** direct signed installer from the MechForge website (engineers in industrial environments often cannot use Store-only distribution).
- **Branding/openness:** public GitHub presence for the project (even if source availability is decided later), product website, engineering-community engagement (forums, LinkedIn, technical content marketing).
- **Early access:** pilot users (Section 7.1) become a feedback panel and the first reviewers.

## 40.3 Sustainability Guardrail

Revenue features must never compromise: offline core operation, formula transparency, or data privacy (Sections 4, 39).

---

# 41. Development Roadmap

Indicative durations assume a small team (1–3 developers). Durations flex with team size; **order is fixed** — each phase gates the next.

## Phase 0 — Foundation (Weeks 1–3)

- Project structure (KMP + Compose)
- Local SQLite + SQLDelight + first migration
- Unit system core
- Calculation engine skeleton
- Domain models
- Navigation shell
- Settings
- Testing framework + CI

## Phase 0.1 — Pilot v0.1 (Weeks 4–10)

- 12 calculators end-to-end on **Desktop only** (Section 7.1)
- History, favorites, export-to-text
- Migration tests
- Pilot acceptance criteria + 5-engineer field test

## Phase 1 — Engine Hardening (Weeks 11–13)

- Expand reference test library
- Property-based tests for unit conversions (round-trip invariants)
- Performance and edge-case pass
- Android shell consuming the same shared module (no full Android UX yet)

## Phase 2 — MVP v1.0 (~40–50 calculators)

- Remaining calculators per Section 7.2
- Full validation + reference suite per calculator
- PDF export on Desktop

## Phase 3 — Engineering References

- Pipe data, materials, HVAC data, engineering constants
- **Only datasets cleared per Section 18 licensing rules**

## Phase 4 — Productivity

- History enhancements, projects, global search
- Excel export

## Phase 5 — Android

- Mobile UI, offline database, Android optimization
- Release preparation

## Phase 6 — Desktop vNext

- Workspaces, batch calculations, advanced reports

## Phase 7 — AI

- Natural-language calculator selection
- Input extraction
- Calculation engine integration
- Result explanation

## Phase 8 — Advanced Platform

Potential future capabilities:

- Equipment selection
- Manufacturer catalogs
- Pump curve analysis
- Psychrometric chart
- System curves
- Engineering report generation
- BIM/Revit integration
- API
- Optional synchronization

---

# 42. Technical Architecture Principle

The software architecture must maintain strict separation:

```text
Presentation
     ↓
Application Services
     ↓
Engineering Calculation Engine
     ↓
Domain Models
     ↓
Local Data Layer
     ↓
SQLite
```

The same engineering logic is reused between Android and Desktop via the **shared Kotlin Multiplatform module** (Section 6).

The project must avoid implementing the same formula separately in each platform.

---

# 43. First Release Non-Goals

The first release is NOT intended to be:

- A CAD system
- A BIM platform
- A CFD solver
- A structural analysis package
- A replacement for engineering standards
- A replacement for manufacturer selection software
- An AI-only application

The first release should be:

> **A reliable, professional Mechanical Engineering Toolkit.**

---

# 44. Success Criteria

MechForge should become the application where a mechanical engineer thinks:

> "Before I search for a calculator, I'll check MechForge."

Success means:

- Fast calculations
- Correct formulas
- Transparent results
- Strong unit handling
- Offline operation
- Professional UI
- Useful history
- Reliable references
- Easy navigation
- Expandable architecture

### Pilot-stage success gates (before v1.0)

- 12/12 pilot calculators pass all acceptance criteria (Section 7.1)
- 5 pilot engineers report weekly usage after 2 weeks
- Zero formula-error reports from pilot
- Engine test coverage ≥ 90% on the shared module

---

# 45. Product Definition

## MechForge

### Mechanical Engineering Toolkit

**One professional toolkit.
Many engineering calculations.
One local engineering workspace.**

MechForge starts as a high-quality calculation suite and evolves toward a complete mechanical engineering productivity platform.

The foundation is:

**Reliable Engineering Calculations + Local-First Data + Cross-Platform Architecture**

The future is:

**Engineering Tools + References + Projects + AI Assistance**

---

# 46. Tagline Candidates

### Option 1
**Engineering calculations. Forged into one toolkit.**

### Option 2
**Your mechanical engineering toolbox.**

### Option 3
**Calculate. Check. Engineer.**

### Option 4
**Precision tools for mechanical engineers.**

### Recommended

> **Calculate. Check. Engineer.**

---

# 47. Golden Development Rule

> **Do not sacrifice engineering correctness for feature quantity.**

One verified calculator with transparent mathematics is more valuable than ten visually impressive calculators with questionable formulas.

Every new calculator must pass:

1. Formula verification
2. Unit verification
3. Input validation
4. Reference verification
5. Automated tests (≥3 reference cases, Section 33)
6. UI review
7. Cross-platform consistency review

before being considered production-ready.

---

## Change Log (v2)

- **Added Section 6:** Tech stack decision (KMP + Compose Multiplatform + SQLDelight) with alternatives and Decision Records process.
- **Added Section 7.1:** Pilot v0.1 — 12 calculators on Desktop with measurable acceptance criteria.
- **Added Section 18:** Reference Data Sources & Licensing rules.
- **Added Section 40:** Business Model & Distribution.
- **Revised Section 41:** Roadmap re-sequenced (pilot before MVP), realistic durations, phase gating.
- **Revised Section 21:** SQLDelight specified; migration tests mandatory.
- **Revised Section 22:** Offline licensing rule.
- **Revised Section 33:** Minimum test coverage rule.
- **Revised Section 38:** Reference dataset metadata (license fields).
- **Revised Section 44:** Pilot-stage success gates.

---

## End of Product README (v2)
