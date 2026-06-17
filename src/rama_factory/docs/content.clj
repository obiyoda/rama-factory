(ns rama-factory.docs.content)

(def pages
  [{:id :vision
    :title "Rama Factory"
    :kicker "Framework Thesis"
    :summary
    "A Laravel-style developer experience for Clojure applications that use Rama for durable state, streaming, materialized views, and fast queries."
    :sections
    [{:heading "Copy the developer experience, not the database model"
      :body
      ["Laravel is the reference point because it makes common application work feel coherent: a CLI, stable project layout, generators, conventions, testing defaults, and a package ecosystem."
       "Rama Factory should do the same for Rama-backed systems while keeping Rama's core concerns visible: depots, PStates, query routing, topology behavior, retries, and read-path cost."]}
     {:heading "The framework boundary"
      :body
      ["The framework owns app shape, generators, web conventions, extension manifests, validation rules, and developer workflow."
       "Rama owns durable state, stream processing, materialized PStates, and queryable data. Zodiac owns the default HTTP runtime."]}
     {:heading "What this docs app proves"
      :body
      ["This site runs as a Rama Factory app instead of a loose static artifact. It uses the same app config, route composition, HTML helpers, JSON helpers, and runtime wrapper that generated applications should use."]}]}

   {:id :architecture
    :title "Architecture"
    :kicker "Default Stack"
    :summary
    "Rama Factory layers conventions over Zodiac and Rama instead of replacing either one."
    :sections
    [{:heading "Request path"
      :bullets
      ["Browser, SPA, CLI, webhook, or worker calls into the application edge."
       "Zodiac handles Ring, Reitit routes, sessions, cookies, CSRF, JSON parsing, Hiccup rendering, and websockets."
       "Application services translate HTTP or UI intent into Rama commands and queries."
       "Rama modules process depots, maintain PStates, and serve queryable state."]}
     {:heading "Code organization"
      :body
      ["The intended app shape is conventional without hiding Clojure. A generated app should make the important directories obvious: modules, HTTP routes, views, policies, jobs, resources, tests, and factory blueprints."]}
     {:heading "Validation gates"
      :body
      ["The existing factory kernel already validates phase ownership, artifact visibility, PState partition keys, bounded growth, query routes, expected I/O, and topology fault-tolerance notes. The web framework should add app-level checks without weakening those Rama checks."]}]}

   {:id :devenv
    :title "Devenv Foundation"
    :kicker "Developer Environment"
    :summary
    "Devenv is the outer developer experience layer: reproducible tools, named tasks, supervised processes, services, and tests."
    :sections
    [{:heading "Why it belongs at the base"
      :body
      ["Laravel's strongest idea is that a project has one obvious way to enter, run, test, and extend it. Devenv gives Rama Factory that outer shell without forcing us to build a process manager, service runner, or tool installer."
       "The framework can generate application code while devenv owns the local environment contract: Clojure tools, Node tooling for SPA clients, long-running processes, readiness checks, and test orchestration."]}
     {:heading "Default commands"
      :bullets
      ["`devenv shell` enters the reproducible development environment."
       "`devenv up` runs the dogfooded docs app as a supervised process."
       "`devenv test` runs the verification suite."
       "`devenv tasks run rama:validate` validates factory and challenge data."
       "`devenv tasks run rama:simulate` generates the sample Rama workflow artifacts."]}
     {:heading "Framework contract"
      :body
      ["Generated Rama Factory apps should include a `devenv.nix` by default. Extensions can contribute tasks, processes, services, and test hooks, while the app still keeps explicit Rama blueprints and Zodiac routes in source control."]}]}

   {:id :swarm
    :title "Swarm Worktrees"
    :kicker "Agent Orchestration"
    :summary
    "SwarmForge gives the role, worktree, tmux, and durable handoff model. Rama Factory should generate and manage that model through devenv."
    :sections
    [{:heading "What SwarmForge contributes"
      :body
      ["SwarmForge's useful core is a small config-driven topology: each `window` line maps a role to an agent backend, a worktree name, and a receive mode."
       "Startup prepares `.worktrees/<role>` checkouts, writes runtime role/session files, syncs helper scripts into each worktree, starts a handoff daemon, and launches agents in tmux sessions. Handoffs are durable files, not direct tmux messages."]}
     {:heading "What devenv should own"
      :bullets
      ["Install and expose prerequisites such as Clojure, Babashka, tmux, git, Node, curl, and ripgrep."
       "Provide named tasks for validation, simulation, swarm config rendering, worktree planning, tests, and future worktree preparation."
       "Supervise noninteractive long-running processes such as the docs server, handoff daemon, local databases, queues, and mock services."
       "Avoid supervising interactive Codex or Claude sessions directly unless they are launched in a noninteractive mode."]}
     {:heading "What Rama Factory should own"
      :bullets
      ["Keep role topology in `factory/factory.edn`."
       "Generate SwarmForge-compatible window config from that topology."
       "Create a worktree plan before mutating git state."
       "Own Rama-aware handoff payloads: run id, phase id, artifact path, commit, validation gate, and next role."
       "Keep destructive operations such as `git worktree add` behind explicit tasks."]}
     {:heading "Worktree flow"
      :body
      ["The main checkout remains the coordinator/specifier surface. Dedicated worktrees such as `.worktrees/coder`, `.worktrees/refactorer`, and `.worktrees/architect` get their own branches and local runtime directories."
       "Agents commit in their assigned worktree. A `git_handoff` names the stable task and commit. The recipient merges or inspects that commit, performs its role, commits any changes, and sends the next handoff."
       "For Rama Factory, the git handoff should carry the factory workflow metadata as well, so a recipient knows both the commit and the Rama phase/artifact gate it is responsible for."]}
     {:heading "Current commands"
      :bullets
      ["`devenv tasks run swarm:plan` prints the role/worktree plan derived from `factory/factory.edn`."
       "`devenv tasks run swarm:config` renders SwarmForge-style `window` lines."
       "`clojure -M:factory swarm-plan` and `clojure -M:factory swarm-config` provide the same outputs without devenv."]}]}

   {:id :zodiac
    :title "Zodiac Runtime"
    :kicker "Default Web Server"
    :summary
    "Zodiac is the default HTTP runtime because it provides the Clojure web baseline while leaving framework conventions to Rama Factory."
    :sections
    [{:heading "Why Zodiac"
      :bullets
      ["Ring and Reitit routing."
       "Embedded Jetty for local development."
       "Hiccup rendering through Chassis."
       "JSON responses, form parsing, cookies, secure sessions, flash messages, file streaming, and websockets."
       "An extension hook based on Integrant config functions."]}
     {:heading "How Rama Factory uses it"
      :body
      ["Application code should depend on `rama-factory.web`, not directly on `zodiac.core`. That keeps Zodiac as the default runtime while preserving room for framework conventions and future adapters."]}
     {:heading "What stays above Zodiac"
      :body
      ["Generators, route conventions, auth flows, hypermedia components, API schemas, TypeScript client generation, and extension manifests belong in Rama Factory. Zodiac should stay the small, reliable HTTP substrate."]}]}

   {:id :frontends
    :title "Frontends"
    :kicker "Hypermedia And SPA"
    :summary
    "The framework should support server-rendered hypermedia and SPA frontends without making either path feel second-class."
    :sections
    [{:heading "Hypermedia lane"
      :bullets
      ["Hiccup pages and fragments."
       "Form helpers, validation errors, redirects, flash messages, and CSRF defaults."
       "Partial refresh patterns compatible with HTMX or Turbo-style clients."
       "Resource pages that can be generated from queries, policies, and view models."]}
     {:heading "SPA lane"
      :bullets
      ["JSON APIs for commands and queries."
       "Generated schemas for request, response, and error payloads."
       "Generated TypeScript client packages."
       "Session or token auth support from the same auth extension."]}
     {:heading "Shared contract"
      :body
      ["Extensions should expose commands, queries, policies, validation rules, view models, optional hypermedia components, and optional API schemas. The frontend mode decides how those capabilities render."]}]}

   {:id :extensions
    :title "Extensions"
    :kicker "Seed Model"
    :summary
    "Rama Factory extensions should feel like high-quality source seeds: pull them in at a point in time, own the copied code, and keep Rama state explicit."
    :sections
    [{:heading "Extension manifest"
      :body
      ["An extension should declare the capabilities it contributes: config keys, generators, blueprints, validators, routes, views, policies, tests, and optional Zodiac config hooks."
       "The first distribution model is shadcn-style copied source, not hidden runtime dependency upgrades. Apps can edit the copied seed however their business needs."]}
     {:heading "Starting extension set"
      :bullets
      ["Auth: users, sessions, tokens, password reset, and passkeys later."
       "Authorization: roles, permissions, policies, and route guards."
       "Tenancy: organizations, workspaces, memberships, and scoped queries."
       "Audit log: append-only activity streams and queryable projections."
       "Jobs and workflows: async work, retries, schedules, and completion state."
       "Files, notifications, billing hooks, search projections, and admin scaffolding."]}
     {:heading "Design rule"
      :body
      ["Extensions may generate useful defaults, but they should never obscure where state lives, how it is partitioned, how retries behave, or what a read path costs."]}]}

   {:id :auth
    :title "Auth Extension"
    :kicker "First Package"
    :summary
    "Auth is the best first seed because it forces the framework to connect real Rama state, sessions, hypermedia screens, JSON APIs, and app-owned source."
    :sections
    [{:heading "Rama responsibilities"
      :bullets
      ["User identity records and credential metadata."
       "Session and token indexes with expiry state."
       "Password reset and login audit events."
       "Query paths for current user, account membership, and policy checks."]}
     {:heading "Web responsibilities"
      :bullets
      ["Login, logout, registration, reset flows, and validation responses."
       "Cookie/session handling through Zodiac."
       "CSRF-safe forms for hypermedia apps."
       "JSON endpoints for SPA clients."]}
     {:heading "Generated surface"
      :body
      ["The first auth seed emits Rama module code, route stubs, view stubs, API schemas, tests, docs, and skill guidance. The target app owns the copied code after install."]}]}

   {:id :factory-dashboard
    :title "Factory Dashboard"
    :kicker "Flagship Seed"
    :summary
    "A Rama-backed factory floor that shows agent work, handoffs, artifacts, validation gates, and timelines while another Rama project is being built."
    :sections
    [{:heading "Control plane and workpiece"
      :body
      ["The dashboard app is the control plane. It observes factory work and materializes views for runs, role queues, artifact status, validation gates, and event timelines."
       "The app being built is the workpiece. It can be a separate generated Rama app such as `invoice-app`, while the factory floor shows the flow of agent work around it."]}
     {:heading "Why Rama fits"
      :bullets
      ["Factory work is naturally event-shaped: run created, handoff accepted, artifact written, validation passed."
       "Rama depots preserve the event stream while PStates provide fast dashboard views."
       "Replay, audit, and future scale are part of the default architecture instead of a later rewrite."]}
     {:heading "Current seed"
      :body
      ["`factory/seeds/factory-dashboard` copies a Rama module, client helpers, Zodiac dashboard routes, tests, docs, and skill guidance into a generated app."
       "The first dashboard route is `/factory`, backed by a demo snapshot when no running agent swarm is connected."]}]}

   {:id :roadmap
    :title "Roadmap"
    :kicker "Dogfood Plan"
    :summary
    "The docs site should keep exercising the framework as each layer lands."
    :sections
    [{:heading "Now"
      :bullets
      ["Run this site through a Rama Factory app config."
       "Serve hypermedia pages and JSON endpoints through the framework web API."
       "Document the framework thesis, Zodiac runtime, frontend lanes, and extension model."]}
     {:heading "Next"
      :bullets
      ["Generate a devenv-backed starter app with `rama new`."
       "Install auth with `rama add auth --from factory/seeds/auth --target <app>`."
       "Install the factory floor with `rama add factory-dashboard --from factory/seeds/factory-dashboard --target <app>`."
       "Tighten the auth seed against Rama `InProcessCluster` behavior."
       "Add an extension manifest validator."
       "Add `make:app`, `make:page`, `make:api`, and `make:extension` commands."
       "Move docs pages into generated resources once generators exist."]}
     {:heading "Later"
      :bullets
      ["Generate a real Rama module from a blueprint."
       "Add local Rama IPC tests."
       "Add SPA client generation and hypermedia component libraries."
       "Publish a package index for extensions."]}]}])

(def pages-by-id
  (into {} (map (juxt :id identity) pages)))

(defn page
  [id]
  (get pages-by-id id))

(defn page-summaries
  []
  (mapv #(select-keys % [:id :title :kicker :summary]) pages))
