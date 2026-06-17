# Rama Auth Reference

The auth seed uses:

- `*registration-depot`, partitioned by email.
- `*session-depot`, partitioned by session id.
- `*session-revocation-depot`, partitioned by session id.
- `$$users`, keyed by user id.
- `$$users-by-email`, keyed by email.
- `$$sessions`, keyed by session id.
- `$$sessions-by-user`, keyed by user id with subindexed session ids.
- `$$login-audit`, keyed by user id with subindexed event ids.

Registration is idempotent by `event-id`. A repeated registration event for the
same email returns the same user id. A different event for an already registered
email is rejected.

Session creation and revocation are idempotent by stable session and event ids.
