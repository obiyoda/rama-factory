---
name: auth-extension
description: Modify the copied Auth extension seed in a Rama Factory app. Use when changing users, credentials, sessions, login audit, auth routes, or auth tests.
---

# Auth Extension

Use this skill after the auth seed has been copied into an app.

## Rules

1. Keep auth state explicit in Rama depots, PStates, topologies, and queries.
2. Preserve idempotency for registration, session creation, and revocation.
3. Do not hide session behavior inside web handlers; handlers should call the
   auth client functions.
4. Replace the demo password hashing before production use.
5. Verify with the app's `devenv test`.

## References

- `references/rama-auth.md`
