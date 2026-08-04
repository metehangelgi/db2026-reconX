# Demo Deck — 10 slides (one purpose each)

TICKET-ADV162. This is the outline/speaker-note content for the deck — build
the actual `docs/demo-deck.pdf` (or `.pptx`) from this table, one slide per
row, capped at three bullets per slide. Everything below is filled in with
this repo's real architecture and results, not placeholders, except where a
`[ ]` marks something only the team can supply (names, who presents what).

| # | Slide | Content (≤3 bullets) | Speaker note |
|---|---|---|---|
| 1 | Title | **ReconX — Enterprise Trade Reconciliation Platform**<br>Deutsche Bank TDI 2026 Advanced Track<br>Team: `[names]` | 10 s max — don't read the title aloud |
| 2 | Problem | Ops reconciles internal trade records against external counterparty/custodian feeds<br>Manual reconciliation is slow and error-prone at volume<br>ReconX automates matching and surfaces breaks for resolution | Anchor in a concrete ops pain point: a missed break costs real money and time to unwind |
| 3 | Architecture | React 19 + Vite frontend, Spring Boot 3 / Java 25 API, PostgreSQL (Liquibase-managed)<br>Kafka event backbone: trade-events → recon/audit/alert consumers, with DLQ<br>Prometheus + Grafana observability | Show the Mermaid runtime diagram from the README; point at each box, name the tech, ~60 s total |
| 4 | Tech stack (by layer) | **Data:** PostgreSQL 16, Liquibase · **App:** Java 25, Spring Boot 3.5, Spring Security/JWT<br>**Messaging:** Apache Kafka (3 topics + DLQ) · **UI:** React 19, Vite, React Router<br>**Observability:** Micrometer, Prometheus, Grafana | Group by layer, don't read as a flat list |
| 5 | Live demo — login + post trade | Switch to the demo laptop: JWT login (role-checked)<br>Post a trade via the AddTrade form (Yup-validated)<br>Show the 201 response and the new row in Trades | Narrate while live: "JWT issued, RBAC-checked, JSR-380 validated, persisted" |
| 6 | Live demo — Kafka + auto-recon | Kafdrop: show the event land on `trade-events`, keyed by tradeRef<br>ReconciliationConsumer picks it up (recon-service group)<br>Grafana panel ticks as the request lands | Show the trade hitting the topic, then the panel moving — cause and effect |
| 7 | CI/CD | Mermaid CI/CD diagram (push → build/test → coverage gate → image → GHCR → deploy)<br>Green GitHub Actions run screenshot<br>Gates: Checkstyle + Liquibase validate on `validate`, 96 tests + ≥85% line coverage on `verify` | Emphasise "Liquibase validate catches changelog drift before tests even run, JaCoCo blocks merges on regressions" |
| 8 | Monitoring | Three Grafana screenshots: baseline / under load / recovery<br>200 VUs, 2 min, k6 — p95 61.8 ms, 0% errors, 46k trades created<br>Kafka consumer lag drains back to zero after load stops | Walk left-to-right: idle → load spike on `/v1/trades` → drain. Name what changed in each panel |
| 9 | Learnings | `[one sentence per team member — hardest bug, biggest win, or what you'd change]` | Honest > polished; every engineer speaks, no passing |
| 10 | Q&A | "Happy to take questions on any layer" + repo URL | Have the repo open in a browser tab, ready to navigate; lead routes questions |

## Notes on slide 8's numbers (from the actual k6 run in this repo)

- **Load:** `loadtest/trade-creation.js`, `constant-vus` executor, 200 VUs, 2 minutes.
- **Result:** 46,446 iterations, 46,447 HTTP requests, 0.00% `http_req_failed`, 0.00% `trade_post_errors`
  (2 requests out of 46,447 hit a non-201 check failure — well within the <2% threshold).
- **Latency:** avg 18.1 ms, p90 41 ms, **p95 55 ms** (threshold was <800 ms), p99 93 ms (threshold was <2000 ms).
- **Throughput:** ~385 req/s sustained.
- Full summary: `loadtest/results/summary.json` / `loadtest/results/run.log`.

This is a genuinely comfortable margin under every threshold — worth saying
explicitly in the demo rather than just "it passed": the backend has real
headroom above 200 concurrent users on this hardware.
