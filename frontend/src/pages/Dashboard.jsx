// TICKET-ADV120 — useMemo for portfolio-value calc.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useEffect, useMemo, useState } from 'react';
import { Line, Doughnut, Bar } from 'react-chartjs-2';
import { withAuth } from '@components/withAuth.jsx';
import PageTopbar from '@components/PageTopbar.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';
import { api } from '@services/apiService.js';
import { marketStatuses } from '../utils/marketHours.js';

function StatCard({ label, value, meta, warn }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
      {meta && <span className={`stat-card__meta${warn ? ' stat-card__meta--warn' : ''}`}>{meta}</span>}
    </article>
  );
}

function useSystemHealth() {
  const [health, setHealth] = useState(null);
  useEffect(() => {
    let cancelled = false;
    api.health()
      .then((res) => { if (!cancelled) setHealth(res); })
      .catch(() => { if (!cancelled) setHealth({ components: {} }); });
    return () => { cancelled = true; };
  }, []);
  return health;
}

const HEALTH_ROWS = [
  { label: 'PostgreSQL',     key: 'reconxDatabase' },
  { label: 'Kafka',          key: 'kafka' },
  { label: 'Spring Boot API',key: null }, // this request succeeding IS the check
];

function Dashboard() {
  const { trades: streamed, isConnected } = useTradeStream();
  const health = useSystemHealth();

  const [historical, setHistorical] = useState([]);
  useEffect(() => {
    api.listTrades('?size=1000')
      .then((res) => setHistorical(res.items))
      .catch(() => setHistorical([]));
  }, []);

  const trades = useMemo(() => {
    const byRef = new Map(historical.map((t) => [t.tradeRef, t]));
    for (const t of streamed) byRef.set(t.tradeRef, t); // live data wins on conflict
    return Array.from(byRef.values());
  }, [historical, streamed]);

  const portfolioValue = useMemo(
    () => trades.reduce((sum, t) => sum + t.quantity * t.price, 0),
    [trades]
  );

  // Trade.status is PENDING/CONFIRMED/SETTLED/CANCELLED — every trade falls
  // into exactly one of these three buckets.
  const { matched, pending, breaks } = useMemo(() => {
    let matchedCount = 0, pendingCount = 0, breaksCount = 0;
    for (const t of trades) {
      if (t.status === 'CONFIRMED' || t.status === 'SETTLED') matchedCount++;
      else if (t.status === 'PENDING') pendingCount++;
      else if (t.status === 'CANCELLED') breaksCount++;
    }
    return { matched: matchedCount, pending: pendingCount, breaks: breaksCount };
  }, [trades]);

  const volumeTrend = useMemo(() => {
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push(d.toISOString().slice(0, 10));
    }
    const counts = Object.fromEntries(days.map((d) => [d, 0]));
    for (const t of trades) {
      if (t.tradeDate in counts) counts[t.tradeDate]++;
    }
    return {
      labels: days.map((d) => new Date(d).toLocaleDateString(undefined, { weekday: 'short' })),
      data: days.map((d) => counts[d]),
    };
  }, [trades]);

  const counterpartyVolume = useMemo(() => {
    const byCp = new Map();
    for (const t of trades) {
      const name = t.counterpartyName || 'Unknown';
      byCp.set(name, (byCp.get(name) || 0) + Number(t.quantity || 0));
    }
    return Array.from(byCp.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6);
  }, [trades]);

  const recentTrades = useMemo(
    () => [...trades].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')).slice(0, 5),
    [trades]
  );

  const markets = useMemo(() => marketStatuses(), []);

  return (
    <section>
      <PageTopbar title="Operations Dashboard" subtitle="Real-time trade reconciliation overview" />

      <div className="stat-grid">
        <StatCard label="Total Trades" value={trades.length} meta="Live + historical" />
        <StatCard
          label="Matched"
          value={matched}
          meta={trades.length ? `${((matched / trades.length) * 100).toFixed(1)}% match rate` : '—'}
        />
        <StatCard label="Open Breaks" value={breaks} meta={breaks > 0 ? 'Requires attention' : 'All clear'} warn={breaks > 0} />
        <StatCard label="Trade Value" value={portfolioValue.toLocaleString(undefined, { maximumFractionDigits: 0 })} meta="Total notional" />
      </div>

      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>

      <div className="chart-grid">
        <div className="chart-card">
          <h3>Trade Volume Trend</h3>
          <div className="chart-card__canvas-wrap">
            <Line
              data={{
                labels: volumeTrend.labels,
                datasets: [{
                  label: 'Trades Processed',
                  data: volumeTrend.data,
                  borderColor: '#0b57d0',
                  backgroundColor: 'rgba(11,87,208,0.15)',
                  borderWidth: 3,
                  fill: true,
                  tension: 0.35,
                  pointRadius: 4,
                  pointBackgroundColor: '#0b57d0',
                }],
              }}
              options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'top' } }, scales: { y: { beginAtZero: true } } }}
            />
          </div>
        </div>

        <div className="chart-card">
          <h3>Match Status</h3>
          <div className="chart-card__canvas-wrap">
            <Doughnut
              data={{
                labels: ['Matched', 'Pending', 'Breaks'],
                datasets: [{
                  data: [matched, pending, breaks],
                  backgroundColor: ['#18864b', '#b78103', '#c62828'],
                  borderWidth: 2,
                }],
              }}
              options={{ responsive: true, maintainAspectRatio: false, cutout: '65%', plugins: { legend: { position: 'bottom' } } }}
            />
          </div>
        </div>
      </div>

      <div className="chart-grid">
        <div className="chart-card">
          <h3>Counterparty Volume</h3>
          <div className="chart-card__canvas-wrap">
            <Bar
              data={{
                labels: counterpartyVolume.map(([name]) => name),
                datasets: [{
                  label: 'Trade Volume',
                  data: counterpartyVolume.map(([, qty]) => qty),
                  backgroundColor: '#0b57d0',
                  borderRadius: 6,
                }],
              }}
              options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }}
            />
          </div>
        </div>

        <div className="chart-card">
          <h3>Global Market Status</h3>
          <table className="info-table">
            <thead><tr><th>Market</th><th>Local Time</th><th>Status</th></tr></thead>
            <tbody>
              {markets.map((m) => (
                <tr key={m.name}>
                  <td>{m.name}</td>
                  <td>{m.localTime}</td>
                  <td><span className={`dot dot--${m.isOpen ? 'up' : 'down'}`} />{m.isOpen ? 'Open' : 'Closed'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <h3 style={{ marginTop: 'var(--space-5)' }}>System Health</h3>
          <table className="info-table">
            <thead><tr><th>Component</th><th>Status</th></tr></thead>
            <tbody>
              {HEALTH_ROWS.map(({ label, key }) => {
                const up = key === null ? !!health : health?.components?.[key]?.status === 'UP';
                return (
                  <tr key={label}>
                    <td>{label}</td>
                    <td><span className={`dot dot--${up ? 'up' : 'down'}`} />{up ? 'Healthy' : 'Unknown'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {recentTrades.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Trade ID</th><th>Instrument</th><th>Counterparty</th><th>Status</th></tr>
            </thead>
            <tbody>
              {recentTrades.map((t) => (
                <tr key={t.tradeRef}>
                  <td>{t.tradeRef}</td>
                  <td>{t.instrumentSymbol}</td>
                  <td>{t.counterpartyName}</td>
                  <td><span className={`status-pill ${t.status.toLowerCase()}`}>{t.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default withAuth(Dashboard);
