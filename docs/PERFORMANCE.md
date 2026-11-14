# Performance

## Machine
CPU: <model, cores> | RAM: <GB> | OS: <name and version> | Docker memory: <GB>
Versions: Java 21, Spring Boot 4.1.x, Kafka 4.0, run via <laptop mvn | compose full profile>

## API (k6, load/api-load.js, rate limit raised to <n>/s)
| Virtual users | Requests/s | p50 | p95 | p99 | Error rate |
| --- | --- | --- | --- | --- | --- |
| 50 | <n> | <ms> | <ms> | <ms> | <%> |

## Stream (simulator, 3 minutes each)
| Events/s | Max consumer lag | Detection latency p95 | Kept up? |
| --- | --- | --- | --- |
| 500 | <n> | <ms> | yes |
| 2,000 | | | |
| 5,000 | | | |
| 10,000 | | | |

## First bottleneck
<what reached its limit first and how you could tell>

## What I would do next
More partitions and detection instances, batching producer sends,
tuning commit.interval.ms, ...