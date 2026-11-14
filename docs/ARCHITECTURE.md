# Architecture

## 1. System context

Who uses the platform and what it talks to.

```mermaid
flowchart LR
    A[Analyst] -->|reviews alerts, works cases| P[Trade Surveillance Platform]
    S[Supervisor] -->|closes cases, runs scenarios| P
    P -->|login, roles| K[Keycloak]
    O[Order flow / simulator] -->|order events| P
```

## 2. Containers

The services and data stores inside the platform.

```mermaid
flowchart LR
    UI[trade-ui React] -->|HTTPS + JWT| GW[gateway]
    GW --> ING[ingestion-service]
    GW --> AC[alert-case-service]
    ING -->|orders.events| KF[(Kafka)]
    KF --> DET[detection-service]
    DET -->|alerts.raw| KF
    KF --> AC
    AC --> PG[(PostgreSQL)]
    AC --> MG[(MongoDB)]
    AC --> RD[(Redis)]
    GW --> RD
    AC -.->|SSE live alerts| UI
```

## 3. Spoofing alert flow

What happens from an order to a live alert on the analyst's screen.

```mermaid
sequenceDiagram
    participant S as Simulator
    participant K as Kafka
    participant D as detection-service
    participant A as alert-case-service
    participant U as Analyst UI
    S->>K: orders.events (key = traderId)
    K->>D: stream of orders
    D->>K: alerts.raw (SPOOFING)
    K->>A: alert (at-least-once)
    A->>A: dedupe (Redis), save (PostgreSQL), evidence (MongoDB)
    A-->>U: live alert over SSE (after commit)
```