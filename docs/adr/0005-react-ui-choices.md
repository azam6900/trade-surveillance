# ADR 0005: React UI choices

## Decision
- Vite + React + TypeScript + React-Bootstrap: fast builds, typed DTOs, accessible
  components without writing CSS.
- TanStack Query for all server data: caching, refetch on invalidate, and
  loading/error states for free; no server data in hand-written state.
- keycloak-js with authorization code + PKCE; the app never sees a password.
- @microsoft/fetch-event-source instead of EventSource, because EventSource
  cannot send the Authorization header our stream requires.
- The UI hides actions a role cannot use, but the server enforces every rule.

## Consequences
+ One API helper adds tokens and turns RFC 7807 errors into messages.
- Environment URLs are baked in at build time; a different host needs a rebuild.