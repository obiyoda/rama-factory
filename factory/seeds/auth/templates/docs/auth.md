# Auth Extension Seed

This app owns the copied auth source. Treat it like shadcn-style generated
code: review it, edit it, and evolve it for the product being built.

## What Was Added

- Rama module code for users, sessions, and login audit.
- Depots for registration, session creation, and session revocation.
- PStates for users, email lookup, sessions, sessions by user, and audit.
- Query topologies for email lookup, current user, and session listing.
- Hypermedia routes for login, registration, and logout.
- JSON endpoint for the current user.
- InProcessCluster tests for Rama behavior.

## Before Production

- Replace `demo-password-hash` with a real password hashing strategy.
- Add CSRF handling appropriate to the web runtime.
- Decide session cookie settings, expiry, rotation, and logout-all policy.
- Extend the manifest if authorization, teams, or passkeys are added.
