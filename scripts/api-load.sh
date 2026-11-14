#!/usr/bin/env bash
# Usage: RATE_LIMIT_PER_SECOND=1000 bash scripts/api-load.sh
# Run the gateway with the raised limit first (see Step 8.5a).
set -euo pipefail

PASS="${PASS:-analyst123}"
LIMIT="${RATE_LIMIT_PER_SECOND:-20}"
OUT=load/summary.json

PASS="$PASS" k6 run \
  --summary-trend-stats="avg,med,p(90),p(95),p(99),max" \
  --summary-export="$OUT" \
  load/api-load.js

echo
echo "### API (k6, load/api-load.js, rate limit raised to ${LIMIT}/s)"
node - "$OUT" <<'EOF'
const fs = require('fs');
const j = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const m = j.metrics;
// k6 versions differ: values sit either directly on the metric or under .values
const get = (o, k) => (o && (o[k] ?? (o.values && o.values[k])));
const d = m.http_req_duration;
const ms = (v) => (v === undefined ? 'n/a' : Math.round(v) + ' ms');
const failed = get(m.http_req_failed, 'rate') ?? get(m.http_req_failed, 'value');
const vus = get(m.vus_max, 'max') ?? get(m.vus_max, 'value');
console.log('| Virtual users | Requests/s | p50 | p95 | p99 | Error rate |');
console.log('| --- | --- | --- | --- | --- | --- |');
console.log(`| ${vus} | ${get(m.http_reqs, 'rate').toFixed(0)} | ${ms(get(d, 'med'))} | ${ms(get(d, 'p(95)'))} | ${ms(get(d, 'p(99)'))} | ${(failed * 100).toFixed(2)}% |`);
EOF