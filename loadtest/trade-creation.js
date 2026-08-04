// ============================================================================
// File: loadtest/trade-creation.js
// TICKET-ADV158 — k6 load test: 200 concurrent users posting trades for 2 min.
//
// Run:  k6 run loadtest/trade-creation.js
//       BASE_URL=http://localhost:8080 k6 run loadtest/trade-creation.js
//
// Payload matches the REAL backend/src/main/java/.../dto/TradeRequest.java
// contract (instrumentId/counterpartyId FKs, assetClass enum, side, and a
// tradeRef matching ^[A-Z]{3}-\d{8}-\d{4}$), not the free-text
// instrumentSymbol/counterpartyLei shape from an earlier draft of this
// script — using seeded instrumentId=1 (SAP.DE) / counterpartyId=1
// (Goldman Sachs International), both present from db/changelog seed data.
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const tradeLatency = new Trend('trade_post_latency_ms');
const errorRate    = new Rate('trade_post_errors');

export const options = {
  scenarios: {
    constant_load: {
      executor:     'constant-vus',
      vus:          200,
      duration:     '2m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'trade_post_latency_ms': ['p(95)<800', 'p(99)<2000'],
    'trade_post_errors':     ['rate<0.02'],
    'http_req_failed':       ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// One-time login, shared across all VUs' iterations.
export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'trader@db.com', password: 'trader123' }),
    { headers: { 'Content-Type': 'application/json' } });
  const token = res.json('token');
  if (!token) {
    throw new Error(`setup() login failed: status=${res.status} body=${res.body}`);
  }
  return { token };
}

export default function (data) {
  // tradeRef must match ^[A-Z]{3}-\d{8}-\d{4}$. The "AAA-YYYYMMDD-NNNN" shape
  // is just a regex, not a validated calendar date, so the two numeric
  // groups are free to encode (VU, ITER) instead of a real date — the
  // (__VU, __ITER) pair is already globally unique for this run, which a
  // 4-digit-only suffix couldn't guarantee at this VU count/duration.
  const vuPart   = String(__VU).padStart(3, '0');
  const iterPart = String(__ITER % 100000).padStart(5, '0');
  const seqPart  = String(Date.now() % 10000).padStart(4, '0');
  const tradeRef = `LTD-${vuPart}${iterPart}-${seqPart}`;

  const payload = JSON.stringify({
    tradeRef,
    instrumentId:   1,
    counterpartyId: 1,
    assetClass:     'EQUITY',
    side:           __VU % 2 === 0 ? 'BUY' : 'SELL',
    quantity:       100 + (__VU % 50),
    price:          245.50 + (__ITER % 10) * 0.01,
    tradeDate:      '2026-06-02',
  });

  const t0 = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/trades`, payload, {
    headers: {
      'Content-Type':  'application/json',
      Authorization:   `Bearer ${data.token}`,
    },
  });
  tradeLatency.add(Date.now() - t0);

  const ok = check(res, {
    '201 created':  (r) => r.status === 201,
    'has trade id': (r) => !!r.json('id'),
  });
  errorRate.add(!ok);

  sleep(0.5);
}
