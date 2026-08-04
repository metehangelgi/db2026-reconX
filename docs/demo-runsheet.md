# Demo Runsheet — 20-minute slot (TICKET-ADV163)

Minute-by-minute timing for the live demo, structured 3 / 8 / 5 / 4 minutes
per the [Final demo](../README.md#final-demo-day-10) breakdown, with every
screen switch in the live-demo block timestamped against this repo's actual
UI flow. Total runs to ~19 minutes, leaving a cushion inside the 20-minute
slot.

Pre-stage before the slot starts:
- `docker compose up -d` already run, all 8 services `(healthy)` — confirm with `docker compose ps`.
- `bash scripts/smoke-test.sh` green (run it in the 10 minutes before your slot, not during).
- Terminal history pre-staged with every command below so nothing is typed live.
- Browser tabs open and ordered: [1] deck, [2] app login (`http://localhost:5173`), [3] Kafdrop (`http://localhost:9000`), [4] Grafana (`http://localhost:3000`), [5] repo on GitHub.
- A screen recording of a full rehearsal run, ready to cut to if the live path breaks.

| Time | Block | What happens | Owner |
|---|---|---|---|
| 0:00–0:30 | Context | Title slide + team intros | `[lead]` |
| 0:30–2:30 | Context | Problem + architecture (slides 2–3) — walk the Mermaid runtime diagram, name each box | `[lead]` |
| 2:30–3:00 | Context | Tech stack (slide 4) — grouped by layer, not read aloud | `[lead]` |
| 3:00–4:00 | Live demo | Switch to tab [2]. Log in as `trader@db.com`. Show the JWT round-trip in DevTools → Network | `[presenter 1]` |
| 4:00–5:30 | Live demo | Open **Add Trade**, submit a trade (RHF + Yup validation visible on a bad field first, then a valid submit). Show the 201 + new row in **Trades** | `[presenter 1]` |
| 5:30–7:00 | Live demo | Switch to tab [3] (Kafdrop) → `trade-events` topic → show the just-published message, keyed by `tradeRef` | `[presenter 2]` |
| 7:00–8:30 | Live demo | Switch to tab [4] (Grafana) → **API request rate by endpoint** panel → point at the `/v1/trades` line ticking up | `[presenter 2]` |
| 8:30–10:00 | Live demo | Switch to a terminal → `docker exec reconx-postgres psql -U reconx_user -d reconx -tAc "SELECT trade_ref, event_type FROM audit_log ORDER BY event_timestamp DESC LIMIT 3;"` — show the audit row that just landed | `[presenter 2]` |
| 10:00–11:00 | Live demo | Back to tab [2] → **Reconciliation** → run a job, resolve an open break, watch the recon metric move | `[presenter 1]` |
| 11:00–12:00 | Transition | Back to slides. "That was the live flow — now the code that makes it work." | `[lead]` |
| 12:00–14:00 | Code walkthrough | `TradeController` / `TradeService` — validation, RBAC, the Kafka publish-after-commit note | `[backend engineer]` |
| 14:00–16:00 | Code walkthrough | `ReconciliationConsumer` / `KafkaErrorHandlerConfig` — DLQ + retry backoff | `[backend engineer]` |
| 16:00–17:00 | Code walkthrough | `useTradeStream` hook / `Dashboard.jsx` — SSE feed + `useMemo`'d aggregations | `[frontend engineer]` |
| 17:00–18:00 | Learnings | One sentence each — hardest bug, biggest win, or what you'd change | whole team |
| 18:00–20:00 | Q&A | Lead routes questions to the right answerer. "We didn't get to that" is a valid answer | whole team |

## Fallback notes for every live step

| If this breaks | Do this instead |
|---|---|
| Login/trade POST fails | Cut to the screen recording from rehearsal; narrate over it |
| Kafka/Kafdrop slow to show the message | Narrate over the wait: "while that lands, here's the topic layout in `KafkaTopicsConfig`" |
| Grafana panel doesn't update | Paste the three saved PNGs from `docs/screenshots/` into the deck slide instead |
| Backend container unhealthy at the start | Have `docker compose logs backend --tail 50` ready; if unrecoverable in <30s, switch to the screen recording |
| A demo laptop network hiccup | Everything runs on `localhost` against the local compose stack — no external network dependency, so this should be rare; if it happens, restart the affected container (`docker compose restart <service>`) |

## Rehearsals (both required before the live slot)

- [ ] **Rehearsal 1** — full run-through with a chaos-monkey interruption partway through the live-demo block (unplug ethernet / close a tab / `Ctrl+C` a running command) to practice recovering without breaking flow.
- [ ] **Rehearsal 2** — full run-through with the trainer asking 2–3 hard Q&A-bank questions at the end.

After each rehearsal, update this table with what slipped and what stuck —
this file should reflect the *actual* rehearsed timing by the live slot, not
just the plan.
