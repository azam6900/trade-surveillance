# ADR 0003: PostgreSQL for records, MongoDB for evidence, Redis as a shortcut

## Context
Alerts and cases are business records that must never be duplicated or lost.
Each detection rule produces evidence of a different shape. Kafka delivers
messages at least once.

## Decision
- PostgreSQL holds alerts, cases, links, and the audit trail. A unique
  alert_key makes a duplicate alert impossible.
- MongoDB holds one evidence document per alert, keyed by the alert id, so
  writing it again replaces it (an upsert).
- Redis holds a 1-hour "seen" key per alert_key. It is only an optimization;
  it is written after the database commit, so losing it never loses data.
- Messages that can never be processed go to alerts.raw.DLT after retries.

## Consequences
+ Replaying the topic is safe; we proved it by resetting the consumer group.
- PostgreSQL and MongoDB are not in one transaction. A crash between them is
  repaired by redelivery (the evidence upsert runs again). A transactional
  outbox would remove this gap; it is out of scope for now.
- Three data stores to run instead of one.