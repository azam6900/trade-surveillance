# Performance

## Machine
CPU: <Intel Core i3-8100, 2 cores> | RAM: <16GB> | OS: <Windows 10 Home Edition> | Docker memory: <8GB>
Versions: Java 21, Spring Boot 4.1.2, Kafka 4.0, run via <laptop mvn | compose full profile>

## API (k6, load/api-load.js, rate limit raised to 1000/s)
| Virtual users | Requests/s | p50 | p95 | p99  | Error rate |
| ---           | ---        | --- | --- | ---  | ---        |
| 50            | 90         |35 ms|293ms|875ms | 0.00%      |

## Stream (simulator, 3 minutes each)
| Events/s | Max consumer lag | Detection latency p95 | Kept up? |
| -------- | ---------------- | --------------------  | -------- |
| 500      | 385              | 1501.8107546813585    | NaN      |
| 2,000    | 19966            | 757.6683324798248     | NaN      |
| 5,000    | 235483           | 835.603368488408      | NaN      |
| 10,000   | 550053           | 1717.024657211793     | NaN      |

## First bottleneck
<what reached its limit first and how you could tell>

## What I would do next
More partitions and detection instances, batching producer sends,
tuning commit.interval.ms, ...