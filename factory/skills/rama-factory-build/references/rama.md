# Rama Application Reference

Use this reference when creating or reviewing Rama module blueprints, generated
Rama artifacts, or validation rules.

## Required Design Surface

Every nontrivial Rama feature should make these explicit:

- command and query contract
- depots and partitioners
- PStates and partition keys
- topology sources and writes
- query routes and expected I/O
- idempotency and retry behavior
- fault-tolerance notes
- acceptance examples
- performance constraints

## Guardrails

- Growing PState collections must be subindexed or explicitly bounded.
- Queries must route to the task owning the relevant PState keys.
- Read paths should avoid hidden fan-out and unbounded scans.
- Retry behavior must be designed before implementation.
- Generated tests should cover idempotency, conservation, ordering, and replay
  where relevant.

## Factory Boundary

Rama owns durable state and materialized views. The web layer owns HTTP,
sessions, forms, redirects, JSON serialization, CSRF, and frontend adapters.
Application services translate web/API intent into Rama commands and queries.
