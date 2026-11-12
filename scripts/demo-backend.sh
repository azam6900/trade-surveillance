#!/usr/bin/env bash
# Runs the Week 4 backend story through the gateway.
# Needs: docker compose, all three services, and the gateway running.
set -euo pipefail

GW=${GW:-http://localhost:8080}
KC=${KC:-http://localhost:8180/realms/trade/protocol/openid-connect/token}

token()  { curl -s -d client_id=trade-ui -d grant_type=password -d username="$1" -d password="$2" "$KC" \
             | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }
field()  { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }     # first string value of a JSON field
patch()  { curl -s -o /dev/null -w "%{http_code}" -X PATCH -H "Authorization: Bearer $1" \
             -H "Content-Type: application/json" -d "$2" "$GW/api/cases/$CASE_ID"; }

echo "1. Getting tokens for analyst1 and supervisor1"
ANALYST=$(token analyst1 analyst123)
SUP=$(token supervisor1 supervisor123)

TRADER="T-$((RANDOM % 900 + 100))"          # a fresh trader each run, so old alerts do not interfere
echo "2. Triggering spoofing for $TRADER"
curl -s -X POST -H "Authorization: Bearer $SUP" "$GW/api/simulate/spoofing?traderId=$TRADER" > /dev/null

echo "3. Waiting for the alert (up to 30 s)"
ALERT_ID=""
for i in $(seq 1 30); do
  ALERT_ID=$(curl -s -H "Authorization: Bearer $ANALYST" "$GW/api/alerts?traderId=$TRADER" | field id || true)
  [ -n "$ALERT_ID" ] && break
  sleep 1
done
[ -z "$ALERT_ID" ] && { echo "   no alert arrived; is detection-service running?"; exit 1; }
echo "   alert $ALERT_ID"

echo "4. Opening a case as analyst1"
CASE_ID=$(curl -s -X POST -H "Authorization: Bearer $ANALYST" -H "Content-Type: application/json" \
  -d "{\"title\":\"Spoofing $TRADER\",\"alertIds\":[\"$ALERT_ID\"]}" "$GW/api/cases" | field id)
echo "   case $CASE_ID"

echo "5. analyst1 moves it to IN_REVIEW  (expect 200): $(patch "$ANALYST" '{"status":"IN_REVIEW","version":0}')"
echo "6. analyst1 tries to close it      (expect 403): $(patch "$ANALYST" '{"status":"CLOSED","version":1}')"
echo "7. supervisor1 closes it           (expect 200): $(patch "$SUP" '{"status":"CLOSED","version":1}')"

echo "8. Audit trail:"
curl -s -H "Authorization: Bearer $ANALYST" "$GW/api/cases/$CASE_ID/events" | grep -o '"actor":"[^"]*","action":"[^"]*"'