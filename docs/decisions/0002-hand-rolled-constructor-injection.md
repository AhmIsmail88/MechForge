# Decision Record 0002 — Hand-rolled constructor injection instead of Koin for the pilot

- **Status:** Accepted
- **Date:** 2026-09-13
- **Context:** README v2 §6.1 allows "Koin (or hand-rolled constructor injection)". The pilot has a small, fixed dependency graph (database, repositories, registry).

## Decision

Use a single `AppDependencies` container constructed in `main.kt` and passed explicitly to screens. No DI framework.

## Consequences

- Positive: zero extra dependencies, trivially debuggable, compile-time visibility of the whole graph.
- Negative: manual wiring when the graph grows; revisit Koin (or alternatives) when Android + more services arrive — that reversal would be recorded here per §6.4.
