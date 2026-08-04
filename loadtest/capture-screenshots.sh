#!/usr/bin/env bash
# TICKET-ADV159 — orchestrates one k6 run alongside the three Grafana
# screenshot captures (baseline is captured separately, before this runs).
set -euo pipefail
cd "$(dirname "$0")/.."

RENDER_URL="http://localhost:3000/render/d-solo/reconx-overview/reconx-overview?panelId=1&width=1000&height=500&tz=UTC&from=now-5m&to=now"

echo "[capture] starting k6 in background..."
BASE_URL=http://localhost:8080 k6 run loadtest/trade-creation.js \
  --summary-export=loadtest/results/summary.json \
  > loadtest/results/run.log 2>&1 &
K6_PID=$!

echo "[capture] waiting 40s for VUs to ramp and generate real load..."
sleep 40
curl -s -u admin:admin "$RENDER_URL" -o docs/screenshots/grafana-under-load.png -w "[capture] under-load size=%{size_download}\n"

echo "[capture] waiting for k6 to finish..."
wait "$K6_PID"
echo "[capture] k6 finished, waiting 30s for the system to settle..."
sleep 30
curl -s -u admin:admin "$RENDER_URL" -o docs/screenshots/grafana-recovery.png -w "[capture] recovery size=%{size_download}\n"

echo "[capture] done."
