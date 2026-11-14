#!/usr/bin/env bash
# Usage: bash scripts/stream-load.sh   (laptop mode, Prometheus on :9090)
PROM=http://localhost:9090
q() {  # q "<promql>"  -> prints one number or n/a
  local v
  v=$(curl -s --data-urlencode "query=$1" "$PROM/api/v1/query" \
      | grep -o '"value":\[[^]]*\]' | sed -E 's/.*,"([^"]*)"\]/\1/')
  echo "${v:-n/a}"
}
echo "| Events/s | Max lag | Consumed/s | Detection p95 (s) |"
echo "| --- | --- | --- | --- |"
for r in 500 2000 5000 10000; do
  source scripts/token.sh supervisor1 supervisor123 > /dev/null
  curl -s -X POST -H "Authorization: Bearer $TOKEN" \
    "localhost:8080/api/simulate/rate?eventsPerSecond=$r" > /dev/null
  sleep 90
  # trigger the spoofing scenario here, with the same command as in Step 3.9
  sleep 90
  lag=$(q 'max(max_over_time(kafka_consumer_fetch_manager_records_lag{application="detection-service"}[3m]))')
  rate=$(q 'sum(rate(kafka_consumer_fetch_manager_records_consumed_total{application="detection-service"}[3m]))')
  p95=$(q 'histogram_quantile(0.95, sum by (le) (increase(alert_detection_latency_seconds_bucket[3m])))')
  echo "| $r | $lag | $rate | $p95 |"
done
source scripts/token.sh supervisor1 supervisor123 > /dev/null
curl -s -X POST -H "Authorization: Bearer $TOKEN" "localhost:8080/api/simulate/rate?eventsPerSecond=20" > /dev/null