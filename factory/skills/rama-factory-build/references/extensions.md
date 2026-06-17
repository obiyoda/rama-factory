# Extension Reference

Use this reference when creating or reviewing extension manifests, generators,
package docs, extension tests, or extension-specific skills.

## Extension Contents

An extension should be able to provide:

- EDN manifest
- Clojure namespaces
- Rama blueprint data
- generators
- validators
- tests
- docs pages
- hypermedia routes/views
- JSON/API schemas
- optional TypeScript client output
- agent skill references

## Manifest Shape

Prefer explicit EDN keys:

- `:extension/id`
- `:extension/name`
- `:extension/status`
- `:extension/summary`
- `:extension/provides`
- `:rama`
- `:web`
- `:generators`
- `:validators`
- `:tests`
- `:skills`

## First-Party Extension Order

Start with boring application needs:

1. auth
2. authorization
3. tenancy
4. audit log
5. jobs and workflows
6. notifications
7. files
8. billing hooks
9. search projections
10. admin scaffolding

## Design Rule

Extensions may generate defaults, but they should not hide state placement,
partitioning, retries, or read-path cost.
