# ADR 0002: Kafka Streams for detection, partitioned by trader

## Context
We must find suspicious patterns per trader in a continuous stream of orders,
quickly, and keep working after restarts.

## Decision
Use Kafka Streams (a library inside detection-service) rather than a separate
stream-processing cluster. Key every order by traderId so all events of one
trader land in the same partition and are handled by one task. Per-trader
windows are then correct without coordination between instances.

## Consequences
+ Windows and counters live in local state stores backed up to Kafka changelog
  topics, so a restarted instance recovers its state.
+ We can scale by adding instances, up to the number of partitions (6).
- Rules that group by a different key (spoofing groups by trader and symbol
  in Week 3) force a repartition topic, which costs extra network and storage.
- We use the record timestamp as event time with no grace period, so late
  events are ignored.
1. Add a data-flow diagram to the README. GitHub draws any fenced code block whose language is mermaid; put these lines inside one:
   flowchart LR
   S[Simulator in ingestion-service] -->|orders.events, key = traderId| K[(Kafka)]
   K --> D[detection-service: Kafka Streams]
   D -->|alerts.raw| K