# Factory Dashboard Seed

This seed turns the app into a Rama-backed factory floor dashboard. It is
copy-owned source: review it, edit it, and adapt the event model for the
software factory being built.

## Why This Seed Exists

Rama Factory assumes successful systems should not need to be rewritten when
coordination, history, audit, replay, or scale become important. The factory
dashboard demonstrates that principle directly:

```text
agent events -> Rama depot -> materialized factory state -> dashboard/API
```

## Event Model

The first event types are:

- `:run-created`
- `:work-item-created`
- `:dependency-added`
- `:handoff-created`
- `:handoff-accepted`
- `:artifact-written`
- `:validation-passed`
- `:validation-failed`
- `:agent-message`
- `:run-digested`

## Views

The Rama module materializes:

- runs by id
- events by run
- handoffs by role
- artifacts by run
- event counts

The default dashboard route is `/factory`.
