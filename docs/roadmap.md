# Rama Factory Roadmap

## Milestone 0: Product Spine

Goal: make the project legible.

- Write the charter.
- Keep the roadmap in the repo.
- Define the first skill-pack shape.
- Draft the first extension manifest.
- Keep devenv as the project entrypoint.

Exit criteria:

- `devenv info` evaluates.
- `devenv test` passes.
- Docs explain the factory, runtime, worktree, and extension boundaries.

## Milestone 1: Factory Kernel

Goal: make the local workflow reliable before adding more surface area.

- Validate factory config, challenge blueprints, extension manifests, and skill
  manifests.
- Prepare git worktrees from `factory/factory.edn`.
- Write per-role runtime metadata.
- Add durable handoffs that can carry both git commit data and Rama phase data.
- Keep destructive worktree actions behind explicit commands.

Exit criteria:

- `devenv tasks run swarm:plan` prints the role/worktree plan.
- `devenv tasks run swarm:prepare` creates worktrees only when safe.
- Handoffs move through `new`, `in-process`, and `completed` states with audit
  metadata.

## Milestone 2: Dogfooded Docs App

Goal: prove the web and docs path by using the framework itself.

- Serve docs through the framework wrapper over Zodiac.
- Support hypermedia pages and JSON docs endpoints.
- Add generated docs pages from extension manifests.
- Keep `devenv up` as the default local command.

Exit criteria:

- `devenv up` starts the docs site.
- The docs site exposes extension and skill metadata.
- Browser and API smoke tests pass.

## Milestone 3: First Extension: Auth

Goal: prove the copy-owned seed model on a boring but real application feature.

- Define auth extension manifest validation.
- Generate a devenv-backed starter app with Rama, Zodiac, tests, and agent skill
  hooks.
- Install auth shadcn-style by copying source into the target app.
- Generate Rama module artifacts for users, sessions, credentials, and audit
  events.
- Generate Zodiac routes for login, logout, registration, current user, and
  session APIs.
- Generate hypermedia views and JSON endpoints from shared contracts.
- Add role skills for implementing and reviewing auth changes.

Exit criteria:

- `rama new sample-app` creates a runnable starter.
- `rama make:extension auth` can scaffold the extension.
- `rama add auth --from <seed>` copies auth into the starter as owned source.
- Generated auth artifacts include tests and validation gates.
- Auth module tests run with Rama `InProcessCluster`.
- The docs app can render the auth extension page from its manifest or seed
  metadata.

## Milestone 4: Factory Floor Dashboard

Goal: make the flagship demo a Rama project that observes factory work flowing
through Rama while building other Rama projects.

- Add `factory-dashboard` as a copy-owned seed.
- Ingest factory events into Rama depots.
- Materialize runs, role handoffs, artifacts, validation gates, timelines, and
  event counts as PStates.
- Render a Zodiac dashboard at `/factory` using Basecoat UI defaults compiled by Vite/Tailwind.
- Keep a demo snapshot available without a running agent swarm.

Exit criteria:

- `rama add factory-dashboard --from <seed>` copies dashboard source into a
  starter app.
- Dashboard module tests run with Rama `InProcessCluster`.
- Generated app tests prove `/factory` and `/api/factory/snapshot`.
- Dashboard markup uses Basecoat classes for cards, badges, buttons, and tables through the asset pipeline.
- Docs explain that the dashboard is a control-plane app for building a
  separate workpiece app.

## Milestone 5: Agent Skill Runtime

Goal: make agents useful without letting them improvise the factory.

- Expose a local stdio MCP server for persona, skill, validation, and handoff
  tools.
- Convert repo-local skills into installable or discoverable skill packs.
- Map factory roles to skills.
- Add role prompts that require relevant skills and reference files.
- Add handoff payloads that tell agents which skill and artifact gate applies.

Exit criteria:

- A feature request can become role-owned tasks.
- MCP-capable agents can discover personas, claim work, and complete handoffs.
- Role agents know which skills and references to load.
- Agent output is committed, handed off, and validated through factory gates.

## Milestone 6: Rama App Framework

Goal: turn the factory into an application framework.

- Add app, module, depot, PState, query, route, page, API, and test generators.
- Add PState schema evolution and backfill planning.
- Add local Rama testing conventions.
- Add deployment metadata.

Exit criteria:

- A new app can be generated and run through devenv.
- A Rama module blueprint can become runnable code and tests.
- The generated app supports both hypermedia and SPA/API frontends.

## Milestone 7: Ecosystem

Goal: make extensions the compounding asset.

- Publish extension manifest schema.
- Add extension registry metadata.
- Build first-party extensions: auth, authorization, tenancy, audit log, jobs,
  notifications, files, billing hooks, search projections, and admin
  scaffolding.
- Add compatibility and verification checks.

Exit criteria:

- Extensions can be installed, generated, documented, tested, and validated.
- Extension docs are generated into the docs app.
- Agent skills can be packaged with extensions.

## Milestone 8: Distribution Surface

Goal: make the open-source seed model easy to consume.

- Public starter repositories.
- Versioned extension seed releases.
- Diff/reseed workflow for apps that want newer extension code.
- Team dashboards for handoffs, validation, runs, and generated artifacts.
- Optional services can come later, but they are not required for the local
  factory workflow.

Exit criteria:

- Developers can start a new app and pull in first-party seeds without a hosted
  service.
- Seed updates are reviewable as source changes, not hidden dependency upgrades.
