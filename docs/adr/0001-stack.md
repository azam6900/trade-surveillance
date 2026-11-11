# ADR 0001: Monorepo, Java 21, Spring Boot 4, Kafka, Keycloak

## Context
We are building a trade surveillance platform made of several services, a
React UI, and shared libraries, and we want one place to build, test, and
deploy everything.

## Decision
- One Git repository (monorepo) with a Maven parent POM and a module per
  service, so shared code is easy to reuse and one CI pipeline builds everything.
- Java 21 (long-term support) and Spring Boot 4 (current generation).
- Kafka for the event stream, because detection needs ordered, replayable events.
- Keycloak as the identity provider, so login, roles, and tokens follow
  standard OAuth 2.0 and OpenID Connect instead of home-made security.

## Consequences
+ Simple local setup: one clone, one `docker compose up`.
+ Shared libraries change in one pull request.
- CI builds everything on every push unless we add path filters later.
- Keycloak adds one more service to run locally and in the cluster.