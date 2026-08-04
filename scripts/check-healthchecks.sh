#!/usr/bin/env bash
# ============================================================================
# TICKET-ADV152 — Verify every compose service's healthcheck reaches `healthy`
# within its retry budget, brought up incrementally so a stuck check is easy
# to isolate.
#
# Run from repo root: bash scripts/check-healthchecks.sh
# ============================================================================
set -euo pipefail

wait_healthy() {
  local name="$1" container="$2" budget_s="$3"
  local waited=0
  echo "  waiting for ${name} (budget ${budget_s}s)..."
  while (( waited < budget_s )); do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo starting)
    if [[ "$status" == "healthy" ]]; then
      echo "  ✓ ${name} healthy (${waited}s)"
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done
  echo "  ✗ ${name} did not reach healthy within ${budget_s}s (last status: ${status})"
  return 1
}

echo "[1/8] postgres..."
docker compose up -d postgres
wait_healthy postgres reconx-postgres 10 || { docker exec reconx-postgres pg_isready -U reconx_user -d reconx; exit 1; }

echo "[2/8] zookeeper..."
docker compose up -d zookeeper
wait_healthy zookeeper reconx-zookeeper 15 || { docker exec reconx-zookeeper nc -z localhost 2181; exit 1; }

echo "[3/8] kafka..."
docker compose up -d kafka
wait_healthy kafka reconx-kafka 40 || { docker exec reconx-kafka kafka-topics --bootstrap-server localhost:9092 --list; exit 1; }

echo "[4/8] backend..."
docker compose up -d backend
wait_healthy backend reconx-backend 90 || { docker exec reconx-backend wget -qO- http://localhost:8080/api/actuator/health; exit 1; }

echo "[5/8] frontend..."
docker compose up -d frontend
wait_healthy frontend reconx-frontend 20 || { docker exec reconx-frontend wget -qO- http://127.0.0.1/; exit 1; }

echo "[6/8] prometheus..."
docker compose up -d prometheus
wait_healthy prometheus reconx-prometheus 20 || { docker exec reconx-prometheus wget -qO- http://localhost:9090/-/healthy; exit 1; }

echo "[7/8] grafana..."
docker compose up -d grafana
wait_healthy grafana reconx-grafana 30 || { docker exec reconx-grafana wget -qO- http://localhost:3000/api/health; exit 1; }

echo "[8/8] kafdrop (debug profile)..."
docker compose --profile debug up -d kafdrop
wait_healthy kafdrop reconx-kafdrop 30 || { docker exec reconx-kafdrop wget -qO- http://localhost:9000/; exit 1; }

echo
echo "All healthchecks green."
