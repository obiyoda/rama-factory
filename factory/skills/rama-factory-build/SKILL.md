---
name: rama-factory-build
description: Build or modify Rama Factory systems, apps, extensions, docs, and role workflows. Use when working on factory orchestration, Rama app blueprints, agent skill packs, Zodiac-backed web surfaces, devenv tasks, extension manifests, or Laravel-style developer experience for Rama.
---

# Rama Factory Build

Use this skill to keep Rama Factory work aligned with the product architecture:
Clojure factory kernel, Rama application model, devenv project runtime, Zodiac
web runtime, role-bound agents, and extension ecosystem.

## Workflow

1. Classify the task as factory kernel, Rama app, web/docs, extension, skill, or
   operations.
2. Read only the relevant reference:
   - Role or handoff work: `references/roles.md`
   - Rama module or blueprint work: `references/rama.md`
   - Extension or ecosystem work: `references/extensions.md`
3. Preserve the layer boundaries:
   - devenv owns shell, tools, services, processes, tests, and task names.
   - Clojure owns factory logic, manifests, validation, and generators.
   - Rama owns durable state, depots, PStates, topologies, queries, and replay.
   - Zodiac owns the default HTTP runtime.
   - Extensions package code, blueprints, validators, tests, docs, and skills.
4. Prefer EDN manifests for structured factory data.
5. Add validation before adding automation that mutates git, worktrees, state, or
   generated app code.
6. Verify with the narrowest useful devenv command.

## Output Conventions

- Keep generated or runtime state out of git: `.devenv/`, `.rama-factory/`,
  `.worktrees/`, and `runs/`.
- Keep docs in `docs/` unless they are rendered app content.
- Keep factory manifests under `factory/`.
- Keep framework code in `src/rama_factory/`.
- Keep tests focused on manifest shape, validation rules, and behavior gates.

## Validation Commands

Use these in order as the work requires:

```bash
devenv tasks run rama:validate
devenv tasks run swarm:plan
devenv tasks run swarm:config
devenv shell -- clojure -M:test
devenv test
```
