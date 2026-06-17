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

For local development, MCP tools append durable EDN events under
`.rama-factory/events`. A copied dashboard app can ingest those files into its
local Rama dashboard module and fall back to demo data when the event log is
empty.

The dashboard route uses Basecoat UI as the default factory screen layer. The
seed includes the Basecoat CDN assets and plain HTML classes such as `card`,
`badge`, `btn-outline`, and `table`.

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

The copied app looks for event logs in this order:

- `RAMA_FACTORY_EVENT_DIR`
- `RAMA_FACTORY_STATE_DIR/events`
- `.rama-factory/events`
- `../.rama-factory/events`
