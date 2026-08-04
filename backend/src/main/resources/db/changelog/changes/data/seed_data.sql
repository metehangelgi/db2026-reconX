-- Classification: Public

-- ============================================================================

-- TICKET-ADV017 — Seed data: 500 trades against the already-seeded

-- counterparties/instruments (loaded from CSV by 008-seed-counterparties /

-- 008-seed-instruments, earlier in this same changelog file). This script

-- deliberately does NOT re-insert counterparties or instruments — doing so

-- collided with the CSV-loaded rows (duplicate symbols/LEI codes) the first

-- time this ran. instrument_id/counterparty_id below are scoped to the 15

-- instruments / 10 counterparties the CSV loaders actually create.

-- ============================================================================



-- 500 trades spread across 4 months (April–July 2026)

INSERT INTO trades (trade_ref, instrument_id, counterparty_id, asset_class, side, quantity, price, trade_date, status)

SELECT

    'TRD-2026-' || LPAD(n::TEXT, 6, '0')                     AS trade_ref,

    i.id                                                       AS instrument_id,

    1 + (n % 10)                                              AS counterparty_id,

    i.asset_class                                             AS asset_class,

    (ARRAY['BUY','SELL'])[1 + (n % 2)]                        AS side,

    ROUND((random() * 10000 + 1)::NUMERIC, 4)                 AS quantity,

    ROUND((random() * 500 + 1)::NUMERIC, 4)                   AS price,

    DATE '2026-04-01' + (n % 120) * INTERVAL '1 day'          AS trade_date,

    (ARRAY['PENDING','CONFIRMED','SETTLED','CANCELLED','CONFIRMED','SETTLED'])[1 + (n % 6)]

                                                              AS status

FROM generate_series(1, 500) AS n

JOIN instruments i ON i.id = 1 + (n % 15);



-- A handful of breaks against a pseudo-random subset of trades

INSERT INTO recon_breaks (trade_id, discrepancy_type, status)

SELECT id,

       (ARRAY['PRICE_MISMATCH','QUANTITY_MISMATCH','DATE_MISMATCH'])[1 + (id % 3)],

       'OPEN'

FROM trades

WHERE id % 17 = 0

LIMIT 30;

 

-- Sanity checks

SELECT COUNT(*) AS counterparties_total FROM counterparties;   -- expect 10 (from CSV)

SELECT COUNT(*) AS instruments_total   FROM instruments;       -- expect 15 (from CSV)

SELECT COUNT(*) AS trades_total        FROM trades;            -- expect 500

SELECT COUNT(*) AS open_breaks         FROM recon_breaks WHERE status = 'OPEN';

 

-- Confirm partition spread:

SELECT

    DATE_TRUNC('month', trade_date)::DATE AS month,

    COUNT(*) AS n

FROM trades

GROUP BY 1

ORDER BY 1;

-- Expect ~125 rows per month across the 4 active partitions.

