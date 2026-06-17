---
name: factory-dashboard
description: Modify the copied Factory Dashboard seed. Use when changing factory events, Rama projections, dashboard routes, agent timelines, handoff metrics, or validation gate views.
---

# Factory Dashboard

Use this skill after the factory-dashboard seed has been copied into an app.

## Rules

1. Treat factory activity as append-only events first.
2. Materialize dashboard views from Rama PStates, not ad hoc in-memory state.
3. Preserve stable event ids for idempotent agent and handoff writes.
4. Keep ephemeral agent execution details separate from permanent run digests.
5. Verify with the app's `devenv test`.

## References

- `references/events.md`
