# ADR 0004: One gateway, JWT checked twice, per-user rate limits

## Context
The browser needs one address. Services must stay safe even if reached
directly. Analysts must not be able to overload the system by accident.

## Decision
- Spring Cloud Gateway (reactive) is the only public entry point. It routes by
  path, applies CORS for the UI origin, and validates the JWT.
- Every service validates the JWT again with the shared trade-security
  library, and enforces roles itself (defense in depth).
- Rate limiting is a Redis token bucket per user (token subject), not per IP.
- Case updates use optimistic locking (version column) and return RFC 7807
  problem details; every change writes an audit row in the same transaction.
- Live alerts use Server-Sent Events, sent only after the database commit.

## Consequences
+ One place for CORS and limits; services stay simple.
+ Losing the gateway's checks (misconfiguration) does not open the services.
- Tokens are verified twice per request (cheap: keys are cached).
- SSE clients are tracked in memory, so live alerts work with ONE instance of
  alert-case-service. Several instances would each need to consume a Kafka
  topic of created alerts and push to their own clients.