#!/usr/bin/env bash
# ============================================================================
# File: scripts/smoke-test.sh
# TICKET-ADV153 — End-to-end smoke test for the full compose stack.
#
# Seven checks: stack up -> login -> trade POST -> Kafka event consumed ->
# Postgres audit row -> Prometheus target UP -> Grafana datasource reachable.
#
# Run from repo root: bash scripts/smoke-test.sh
# ============================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TRADE_REF="SMK-$(date +%Y%m%d)-$(printf '%04d' $((RANDOM % 10000)))"

fail() { echo "  ✗ $1"; exit 1; }

echo "▶ 1/7  Bringing the stack up..."
docker compose up -d
echo "  waiting up to 90s for backend to be healthy..."
status="starting"
for _ in $(seq 1 18); do
  status=$(docker inspect --format='{{.State.Health.Status}}' reconx-backend 2>/dev/null || echo starting)
  [[ "$status" == "healthy" ]] && break
  sleep 5
done
[[ "$status" == "healthy" ]] || fail "backend not healthy (status: $status)"
echo "  ✓ backend healthy"

echo "▶ 2/7  Logging in as trader..."
LOGIN_RESPONSE=$(curl -fsS -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"trader@db.com","password":"trader123"}') || fail "login request failed"
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r .token)
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail "login failed — no token in response: $LOGIN_RESPONSE"
echo "  ✓ JWT acquired"

# Snapshot the topic's total message count *before* posting, so the Kafka
# check below can bound the consumer by exact message count instead of
# racing an idle-timeout.
PRE_COUNT=$(docker exec reconx-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic trade-events 2>/dev/null \
  | awk -F: '{sum+=$3} END {print sum}')

echo "▶ 3/7  Posting a trade ($TRADE_REF)..."
TRADE=$(curl -fsS -X POST "$BASE_URL/api/v1/trades" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"tradeRef\":\"$TRADE_REF\",\"instrumentId\":1,\"counterpartyId\":1,\"assetClass\":\"EQUITY\",\"side\":\"BUY\",\"quantity\":100,\"price\":245.50,\"tradeDate\":\"2026-06-02\"}") \
  || fail "trade POST failed"
TRADE_ID=$(echo "$TRADE" | jq -r .id)
[[ -n "$TRADE_ID" && "$TRADE_ID" != "null" ]] || fail "trade POST returned no id: $TRADE"
echo "  ✓ trade created: id=$TRADE_ID ref=$TRADE_REF"

echo "▶ 4/7  Confirming Kafka event on trade-events..."
sleep 2 # producer.send() is async — give it a moment to land before we consume
# IMPORTANT: capture the full output into a variable *before* grepping it.
# Piping straight into `grep -q` lets grep exit the instant it matches,
# SIGPIPE-ing kafka-console-consumer — and under `set -o pipefail` that
# upstream non-zero exit fails the whole pipeline even though grep found
# the match. Capturing first avoids that entirely.
KAFKA_OUTPUT=$(docker exec reconx-kafka kafka-console-consumer \
  --bootstrap-server kafka:29092 --topic trade-events \
  --from-beginning --max-messages "$((PRE_COUNT + 1))" --timeout-ms 30000 2>/dev/null || true)
if grep -q "$TRADE_REF" <<< "$KAFKA_OUTPUT"; then
  echo "  ✓ trade-event found on topic"
else
  fail "no Kafka event found for $TRADE_REF"
fi

echo "▶ 5/7  Confirming Postgres audit row..."
COUNT=$(docker exec reconx-postgres psql -U reconx_user -d reconx -tAc \
  "SELECT COUNT(*) FROM audit_log WHERE trade_ref='$TRADE_REF';" | tr -d '[:space:]')
[[ "$COUNT" != "0" && -n "$COUNT" ]] || fail "no audit row for $TRADE_REF (count=$COUNT)"
echo "  ✓ audit row present (count=$COUNT)"

echo "▶ 6/7  Confirming Prometheus scrape..."
PROM_UP=$(curl -fsS "http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22reconx-backend%22%7D" \
  | jq -r '.data.result[0].value[1] // "0"')
[[ "$PROM_UP" == "1" ]] || fail "Prometheus target reconx-backend is DOWN (value=$PROM_UP)"
echo "  ✓ Prometheus scraping backend"

echo "▶ 7/7  Confirming Grafana datasource..."
GRAFANA_UID=$(curl -fsS -u admin:admin http://localhost:3000/api/datasources/uid/reconx-prometheus \
  | jq -r '.uid // empty')
[[ "$GRAFANA_UID" == "reconx-prometheus" ]] || fail "Grafana datasource reconx-prometheus not provisioned"
echo "  ✓ Grafana datasource provisioned"

echo
echo "✅  All 7 checks green — stack is demo-ready."
