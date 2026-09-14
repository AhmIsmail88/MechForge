# Decision Record 0001 — Pilot v0.1 is Desktop-only; Android target deferred to Phase 1

- **Status:** Accepted
- **Date:** 2026-09-13
- **Context:** README v2 §4.4 and §7.1 define a pilot release (12 calculators) that must prove the full vertical slice on one platform before scaling. §41 places the Android shell in Phase 1, after engine hardening.

## Decision

The pilot codebase ships with JVM targets only:

- `core` — Kotlin Multiplatform module with a `jvm` target (pure Kotlin engine, units, domain).
- `composeApp` — Kotlin Multiplatform module with a `jvm("desktop")` target (Compose Multiplatform UI, SQLDelight persistence).

No Android (AGP) target is configured during Phase 0 / Pilot v0.1.

## Consequences

- Positive: far less build-system risk (no AGP/SDK), faster test cycles, the pilot gates on Desktop acceptance criteria exactly as specified.
- Positive: adding the Android target later is a target declaration plus platform entry point; the shared engine does not change.
- Negative: "cross-platform" acceptance (§47 item 7) cannot be demonstrated until Phase 1; this record is the tracking point for that deferral.
- The database layer (SQLDelight) uses a desktop JDBC SQLite driver now; the Android driver swap is isolated in `DatabaseFactory`.
