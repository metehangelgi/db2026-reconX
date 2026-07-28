-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
-- ============================================================================
SELECT DISTINCT
    t.instrument_id,
    t.trade_date,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap
FROM trades t
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;


-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle rollup
--                 EXECUTION -> CONFIRMATION -> SETTLEMENT -> RECON_BREAK
--                 -> RESOLUTION  (one row per stage per trade, max 5)
--
-- The lifecycle is not a self-FK chain on trades — it spans three tables. The
-- recursive step therefore reads whichever table produces the next event, via
-- correlated scalar subqueries selected by a CASE on the current stage. Each
-- subquery yields at most one value, so a trade with N settlements advances by
-- exactly one row per step instead of fanning out into N^k rows.
--
-- Termination: WHERE tl.stage < 5 caps the walk at five stages, and the EXISTS
-- guards stop a trade whose next-stage event does not exist (an unsettled trade
-- simply ends at CONFIRMATION).
--
-- Portability: standard SQL only — CAST(...) rather than the Postgres `::`
-- operator, FETCH FIRST rather than LIMIT, an explicit CTE column list (H2
-- requires it on a recursive CTE), and no LATERAL (H2 2.3.232 does not support
-- it at all). Verified on H2 2.3.232 (MODE=PostgreSQL) — the version this
-- project resolves. Nothing here is H2-specific, so it runs on Postgres too.
-- ============================================================================
WITH RECURSIVE trade_lifecycle (
    trade_id, trade_ref, stage, stage_name, event_at, event_status
) AS (
    -- Base case — stage 1: every live trade enters the lifecycle as EXECUTION.
    SELECT
        t.id                               AS trade_id,
        t.trade_ref                        AS trade_ref,
        1                                  AS stage,
        CAST('EXECUTION' AS VARCHAR)       AS stage_name,
        t.created_at                       AS event_at,
        CAST(t.status AS VARCHAR)          AS event_status
    FROM trades t
    WHERE t.deleted_at IS NULL

    UNION ALL

    -- Recursive step — for a trade sitting at stage N, emit its stage N+1 event.
    SELECT
        tl.trade_id,
        tl.trade_ref,
        tl.stage + 1,
        CAST(CASE tl.stage
                 WHEN 1 THEN 'CONFIRMATION'   -- booked trade acknowledged
                 WHEN 2 THEN 'SETTLEMENT'     -- earliest settlement booked
                 WHEN 3 THEN 'RECON_BREAK'    -- first mismatch raised
                 ELSE         'RESOLUTION'    -- that break closed out
             END AS VARCHAR),
        CASE tl.stage
            WHEN 1 THEN (SELECT COALESCE(t.modified_at, t.created_at)
                         FROM trades t
                         WHERE t.id = tl.trade_id)
            WHEN 2 THEN (SELECT CAST(s.settlement_date AS TIMESTAMP)
                         FROM settlements s
                         WHERE s.trade_id = tl.trade_id
                         ORDER BY s.settlement_date, s.id
                         FETCH FIRST 1 ROW ONLY)
            WHEN 3 THEN (SELECT b.detected_at
                         FROM recon_breaks b
                         WHERE b.trade_id = tl.trade_id
                         ORDER BY b.detected_at, b.id
                         FETCH FIRST 1 ROW ONLY)
            ELSE        (SELECT b.resolved_at
                         FROM recon_breaks b
                         WHERE b.trade_id = tl.trade_id
                           AND b.resolved_at IS NOT NULL
                         ORDER BY b.resolved_at, b.id
                         FETCH FIRST 1 ROW ONLY)
        END,
        CAST(CASE tl.stage
            WHEN 1 THEN (SELECT t.status
                         FROM trades t
                         WHERE t.id = tl.trade_id)
            WHEN 2 THEN (SELECT s.status
                         FROM settlements s
                         WHERE s.trade_id = tl.trade_id
                         ORDER BY s.settlement_date, s.id
                         FETCH FIRST 1 ROW ONLY)
            WHEN 3 THEN (SELECT b.status
                         FROM recon_breaks b
                         WHERE b.trade_id = tl.trade_id
                         ORDER BY b.detected_at, b.id
                         FETCH FIRST 1 ROW ONLY)
            ELSE        (SELECT b.status
                         FROM recon_breaks b
                         WHERE b.trade_id = tl.trade_id
                           AND b.resolved_at IS NOT NULL
                         ORDER BY b.resolved_at, b.id
                         FETCH FIRST 1 ROW ONLY)
        END AS VARCHAR)
    FROM trade_lifecycle tl
    WHERE tl.stage < 5                        -- termination guard
      -- and only advance if the next stage actually has an event
      AND (   tl.stage = 1
           OR (tl.stage = 2 AND EXISTS (SELECT 1 FROM settlements s
                                        WHERE s.trade_id = tl.trade_id))
           OR (tl.stage = 3 AND EXISTS (SELECT 1 FROM recon_breaks b
                                        WHERE b.trade_id = tl.trade_id))
           OR (tl.stage = 4 AND EXISTS (SELECT 1 FROM recon_breaks b
                                        WHERE b.trade_id = tl.trade_id
                                          AND b.resolved_at IS NOT NULL))
          )
)
SELECT
    trade_id,
    trade_ref,
    stage,
    stage_name,
    event_at,
    event_status
FROM trade_lifecycle
ORDER BY trade_id, stage;


-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view (concurrent so it can
--         run while the dashboard is reading it)
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;


-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT id, symbol, metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;
