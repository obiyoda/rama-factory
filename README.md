# Rama Factory PoC

A Clojure proof of concept for a software factory that combines two ideas:

- SwarmForge-style local role orchestration, durable handoff files, and explicit ownership.
- rama-ai-learn-style Rama challenge blueprints, phased artifacts, and validation gates.

This repository does not launch real agent terminals or deploy a production Rama cluster yet. It provides the Clojure kernel those pieces can sit on: factory configuration, challenge data, validation rules, generated artifacts, seed generators, and a file-backed handoff queue.

It also includes the first dogfooded web app: a Rama Factory docs site that runs through a small framework wrapper over [Zodiac](https://github.com/brettatoms/zodiac). The docs app uses framework app config, route composition, Hiccup rendering, JSON endpoints, and the default Zodiac runtime rather than being a static artifact.

The project-level developer experience is intended to start with [devenv](https://devenv.sh/). Devenv provides the reproducible shell, tools, named tasks, supervised processes, readiness checks, and test entrypoint. Zodiac is the default web runtime inside that environment.

## What It Builds

The product direction is captured in:

- [`docs/charter.md`](docs/charter.md) for the mission, principles, and economic strategy.
- [`docs/roadmap.md`](docs/roadmap.md) for staged milestones.
- [`factory/skills/rama-factory-build/`](factory/skills/rama-factory-build/) for the first repo-local agent skill pack.
- [`factory/personas.edn`](factory/personas.edn) for named agent personas such as Snips and ArchitectAlice.
- [`factory/extensions/auth.edn`](factory/extensions/auth.edn) for the first extension manifest draft.
- [`factory/seeds/auth/`](factory/seeds/auth/) for the first copy-owned extension seed.
- [`factory/seeds/factory-dashboard/`](factory/seeds/factory-dashboard/) for the flagship factory-floor dashboard seed.

The sample factory runs an eight-phase Rama module workflow:

1. implicit specification
2. Rama module plan
3. plan validation
4. implementation slot
5. implementation validation
6. test slot
7. test validation
8. finish

The role topology is intentionally small:

- `specifier` owns external behavior and acceptance criteria.
- `architect` owns plan and adversarial design validation.
- `coder` owns implementation, tests, and the final green loop.
- `refactorer` owns structural/test validation.

The sample challenge is `factory/challenges/bank-transfer.edn`, modeled after the Rama bank transfer challenge shape: protocol operations, depots, PStates, queries, topology fault-tolerance notes, acceptance cases, and non-functional constraints.

## Commands

Preferred workflow:

```bash
devenv shell
devenv up
devenv test
devenv tasks run rama:validate
devenv tasks run rama:simulate
devenv tasks run swarm:plan
devenv tasks run swarm:config
devenv tasks run assets:build
clojure -M:mcp
```

`devenv up` starts the dogfooded docs app at `http://localhost:3000`.
`assets:build` compiles the Vite/Tailwind/Basecoat asset entry.

Starter and seed commands:

```bash
clojure -M:factory new invoice-app
clojure -M:factory add auth --from factory/seeds/auth --target invoice-app
cd invoice-app
pnpm install --frozen-lockfile
pnpm run assets:build
devenv test
```

Factory-floor dashboard demo:

```bash
clojure -M:factory new factory-floor
clojure -M:factory add factory-dashboard --from factory/seeds/factory-dashboard --target factory-floor
cd factory-floor
devenv up
```

Generated apps run a Clojure web process and a Vite asset process under
`devenv up`. The dashboard uses `RAMA_FACTORY_VITE_ORIGIN`, defaulting to
`http://localhost:5173`, to load the Vite entry during local development.

From this repository, the auth seed can be validated with:

```bash
clojure -M:factory make:extension auth
```

Raw Clojure commands are still available underneath the devenv layer:

```bash
clojure -M:factory new invoice-app
clojure -M:factory make:extension auth
clojure -M:factory make:extension factory-dashboard
clojure -M:factory add auth --from <seed-path> --target <app-dir>
clojure -M:factory add factory-dashboard --from <seed-path> --target <app-dir>
clojure -M:factory validate
clojure -M:factory simulate demo-bank-transfer
clojure -M:factory swarm-plan
clojure -M:factory swarm-config
clojure -M:factory personas
clojure -M:factory persona snips
clojure -M:factory mcp-tools
clojure -M:factory queue
clojure -M:factory accept architect
clojure -M:factory complete architect <handoff-id>
pnpm run assets:dev
pnpm run assets:build
clojure -M:mcp
clojure -M:docs
clojure -M:test
```

`simulate` writes generated artifacts under `runs/<run-id>/` and handoff queue state under `.rama-factory/`. Both are ignored by git.

`clojure -M:docs` starts the dogfooded docs app directly. Pass a port to override it:

```bash
clojure -M:docs 3001
```

## Local MCP Adapter

Rama Factory includes a local stdio MCP server for agents that support MCP. It
wraps the existing EDN factory kernel rather than creating a second workflow
model.

```json
{
  "mcpServers": {
    "rama-factory": {
      "command": "clojure",
      "args": ["-M:mcp"],
      "cwd": "/path/to/rama-factory"
    }
  }
}
```

The first MCP tools expose the core control-plane loop:

- `factory.list_personas`
- `factory.get_persona`
- `factory.list_skills`
- `factory.get_skill`
- `factory.queue_summary`
- `factory.create_work`
- `factory.claim_next_work`
- `factory.complete_work`
- `factory.validate`

## Design Boundary

This PoC deliberately keeps the first useful core small:

- It keeps the core small: EDN for factory state, plus `org.clojure/data.json` for the MCP JSON-RPC transport.
- It validates Rama-specific design metadata before generating a run.
- It models SwarmForge handoffs as durable EDN files with `new`, `in-process`, `completed`, and audit queues.
- It generates visible artifacts for every Rama development phase so skipped phases become obvious.
- It treats devenv as the project entrypoint for shell tooling, tasks, processes, and tests.
- It can render a SwarmForge-style role/window config and a git worktree plan from `factory/factory.edn`.
- It defines named personas so humans can see who did the work, what skills they use, and how handoffs/events should be attributed.
- It treats Zodiac as the default web runtime while keeping application code behind `rama-factory.web`.
- It supports both hypermedia pages and JSON endpoints in the first dogfooded app.
- It can generate a devenv-backed starter app.
- It can install the auth extension as shadcn-style copied source that the target app owns.
- It can install a Rama-backed factory dashboard seed that shows agent/factory events, handoffs, artifacts, validation gates, and timelines.
- It exposes a local stdio MCP server so MCP-capable agents can discover personas, load skills, validate the factory, and move work through durable handoff queues.
- MCP create/claim/complete calls append durable events under `.rama-factory/events`, which the factory dashboard seed can ingest into its Rama module.
- Factory-facing UI screens use Basecoat UI defaults through a Vite/Tailwind asset pipeline: plain HTML, shadcn-style classes, and no React requirement.

The next practical increment is adding `swarm:prepare` so role worktrees can operate on starter apps and seeds.
