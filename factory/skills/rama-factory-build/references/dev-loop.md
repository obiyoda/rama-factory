# Dev Loop Reference

Use this reference when changing Clojure routes/views, dashboard screens,
Basecoat classes, Tailwind utilities, Vite assets, or live-preview behavior.

## Two Hot Loops

Rama Factory development has two separate feedback loops:

- Clojure loop: keep the app running, evaluate changed namespaces/functions in
  a REPL, refresh the browser or call handlers directly, then run narrow tests.
- Asset loop: keep Vite running so Tailwind and Basecoat classes compile while
  the browser reloads or hot-updates CSS and JavaScript.

Do not restart the Clojure process just to change HTML, route helpers, or pure
functions if evaluating the namespace is enough. Do restart when dependencies,
ports, process env, or Rama module topology changes require it.

## Commands

Use these as the default local workflow:

```bash
devenv up
devenv tasks run assets:build
pnpm run assets:dev
clojure -M:factory project-list
clojure -M:factory project-validate
clojure -M:factory lab-validate factory-floor
devenv shell -- clojure -M:test
```

Generated apps run the web process and the Vite asset process together under
`devenv up`. The Vite dev server defaults to:

```text
http://localhost:5173
```

Dashboard views load assets from `RAMA_FACTORY_VITE_ORIGIN` when set, otherwise
from `http://localhost:5173`.

Use pnpm for JavaScript dependencies. `devenv` includes `pkgs.pnpm`, generated
apps include `pnpm-lock.yaml`, and asset tasks use frozen pnpm installs.

Use `factory/projects.edn` and `factory/seed-labs/*.edn` for dogfooding generated
apps. Active lab apps belong under ignored `.rama-workspaces/`, not at the
factory repo root.

## UI Defaults

- Use Basecoat classes for factory-facing screens.
- Keep markup plain HTML or Hiccup-compatible string rendering unless a richer
  frontend is explicitly needed.
- Put Tailwind/Basecoat imports in `assets/app.css`.
- Put JavaScript behavior in `assets/app.js`.
- Do not hand-code one-off CSS when a Basecoat component class or Tailwind
  utility is enough.
