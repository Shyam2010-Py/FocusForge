# FocusForge architecture

## Principles
- Local-first: timer operation and personal settings must work without a network connection.
- Cloud only for account sync and shared rooms.
- No paid-only feature gates, advertising SDKs or subscription code.
- No per-second database writes for countdown synchronization.

## Android layers
- `ui`: Jetpack Compose screens and reusable components.
- `domain`: timer, planner and session rules (to be added).
- `data/local`: Room entities, DAOs and repositories (to be added).
- `data/remote`: Supabase Auth, PostgREST and Realtime adapters (to be added).

## Multiplayer clock model
The host writes `started_at` and `duration_seconds` once. Participants calculate remaining time locally from the authoritative start timestamp and duration. Pause/resume should be explicit state transitions, not timer tick writes.

## Trust boundaries
- Supabase publishable key may be included in the client.
- Secret/service-role keys must remain server-side.
- RLS is mandatory on exposed tables.
- Validate room capacity, host privileges, membership and state transitions on the server/database, not only in UI code.
