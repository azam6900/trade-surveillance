#!/usr/bin/env bash
# Usage: source scripts/token.sh analyst1 analyst123   -> sets $TOKEN
USER=${1:-analyst1}; PASS=${2:-analyst123}
TOKEN=$(curl -s -d client_id=trade-ui -d grant_type=password \
  -d username=$USER -d password=$PASS \
  http://localhost:8180/realms/trade/protocol/openid-connect/token \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
export TOKEN
echo "token for $USER, length ${#TOKEN}"