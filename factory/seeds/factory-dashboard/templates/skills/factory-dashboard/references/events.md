# Factory Event Reference

The dashboard seed models the software factory as events flowing through Rama.

Required event fields:

- `:event-id`
- `:event-type`
- `:run-id`
- `:work-id`
- `:role`
- `:phase`
- `:artifact`
- `:status`
- `:message`
- `:occurred-at`

Use content-addressed or UUID-derived ids for concurrent agent events. Avoid
sequential ids when multiple agents or worktrees can write independently.

Temporary agent steps can be recorded as local-only execution events and later
compressed into a permanent run digest.
