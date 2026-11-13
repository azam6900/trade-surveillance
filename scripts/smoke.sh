#!/usr/bin/env bash
# Waits for every application rollout, then calls each readiness URL from inside the cluster.
set -euo pipefail
NS=${NS:-trade}

for d in alert-case-service ingestion-service detection-service gateway trade-ui; do
  kubectl -n "$NS" rollout status deploy/"$d" --timeout=180s
done

for target in alert-case-service:8081 ingestion-service:8082 detection-service:8083 gateway:8080; do
  name=${target%%:*}; port=${target##*:}
  result=$(kubectl -n "$NS" get --raw "/api/v1/namespaces/$NS/services/$name:$port/proxy/actuator/health/readiness")
  echo "$name: $result"
  [[ "$result" == *'"UP"'* ]] || { echo "FAILED: $name"; exit 1; }
done
echo "All services ready."