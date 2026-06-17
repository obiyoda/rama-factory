# Role And Handoff Reference

Use this reference when changing role topology, worktree behavior, agent prompts,
handoff payloads, or factory workflow phases.

## Roles

- `specifier`: owns externally visible behavior, acceptance criteria, and user
  approval before implementation handoff.
- `architect`: owns Rama plan quality, module boundaries, partitioning,
  topology design, and implementation validation.
- `coder`: owns implementation, focused tests, and the final green loop.
- `refactorer`: owns behavior-preserving cleanup, structure review, and test
  validation.

## Worktrees

The main checkout is the coordinator/specifier surface. Implementation and
review roles should use dedicated worktrees:

```text
.worktrees/coder
.worktrees/refactorer
.worktrees/architect
```

Do not create or reset worktrees implicitly. Destructive or branch-changing
operations need explicit commands with preflight checks.

## Handoffs

Handoffs should be durable files with audit metadata. A Rama Factory handoff
should be able to carry:

- sender and recipient role
- priority
- receive mode
- task name
- run id
- phase id
- artifact path
- gate id
- optional git commit
- result status

Agents should not hand-edit runtime queue state. They should accept, complete,
or create handoffs through helper commands.
