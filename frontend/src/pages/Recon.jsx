// TICKET-ADV068-070 (frontend) — trigger a recon run, view its results, resolve breaks.
import React, { useState, useCallback, useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import PageTopbar from '@components/PageTopbar.jsx';
import { api } from '@services/apiService.js';

const COLUMNS = [
  { key: 'tradeRef',         label: 'Trade ID' },
  { key: 'instrumentSymbol', label: 'Instrument' },
  { key: 'counterpartyName', label: 'Counterparty' },
  { key: 'quantity',         label: 'Quantity' },
  { key: 'discrepancyType',  label: 'Discrepancy' },
  { key: 'priority',         label: 'Priority' },
  { key: 'status',           label: 'Status' },
  { key: 'detectedAt',       label: 'Detected' },
  { key: 'action',           label: '' },
];

const PAGE_SIZE = 10;

function Recon() {
  const [from, setFrom] = useState('2026-01-01');
  const [to, setTo] = useState('2026-12-31');
  const [jobId, setJobId] = useState(null);
  const [job, setJob] = useState(null);
  const [breaks, setBreaks] = useState([]);
  const [error, setError] = useState(null);
  const [resolvingId, setResolvingId] = useState(null);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [counterpartyFilter, setCounterpartyFilter] = useState('');
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');

  async function runRecon(e) {
    e.preventDefault();
    setError(null);
    try {
      const res = await api.runRecon({ from, to });
      setJobId(res.jobId);
      setJob(null);
      setBreaks([]);
      setPage(0);
    } catch (err) {
      setError(err.message);
    }
  }

  const loadResults = useCallback(async () => {
    if (!jobId) return;
    setLoading(true);
    setError(null);
    try {
      const [jobRes, breaksRes] = await Promise.all([api.reconJob(jobId), api.reconResults(jobId)]);
      setJob(jobRes);
      setBreaks(breaksRes);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [jobId]);

  async function resolveBreak(id) {
    const note = window.prompt('Resolution note:');
    if (note === null) return;
    setResolvingId(id);
    setError(null);
    try {
      const updated = await api.resolveBreak(id, note);
      setBreaks((prev) => prev.map((b) => (b.id === id ? updated : b)));
    } catch (err) {
      setError(err.message);
    } finally {
      setResolvingId(null);
    }
  }

  const handleSortChange = useCallback((key) => {
    if (key === 'action') return;
    setSortDir((prevDir) => (sortKey === key ? (prevDir === 'asc' ? 'desc' : 'asc') : 'asc'));
    setSortKey(key);
  }, [sortKey]);

  const counterparties = useMemo(
    () => [...new Set(breaks.map((b) => b.counterpartyName).filter(Boolean))].sort(),
    [breaks]
  );

  const visibleBreaks = useMemo(() => {
    let items = breaks.filter((b) =>
      (!statusFilter || b.status === statusFilter) &&
      (!priorityFilter || b.priority === priorityFilter) &&
      (!counterpartyFilter || b.counterpartyName === counterpartyFilter)
    );
    if (search) {
      const needle = search.toLowerCase();
      items = items.filter((b) => (b.tradeRef || '').toLowerCase().includes(needle));
    }
    if (!sortKey) return items;
    const dir = sortDir === 'asc' ? 1 : -1;
    return [...items].sort((a, b) => {
      const av = a[sortKey];
      const bv = b[sortKey];
      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
      return String(av).localeCompare(String(bv)) * dir;
    });
  }, [breaks, statusFilter, priorityFilter, counterpartyFilter, search, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(visibleBreaks.length / PAGE_SIZE));
  const pagedBreaks = visibleBreaks.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  const recentActivity = useMemo(() => {
    const events = [];
    for (const b of breaks) {
      if (b.detectedAt) events.push({ ts: b.detectedAt, text: `Break detected for ${b.tradeRef} — ${b.priority} priority (${b.discrepancyType.replaceAll('_', ' ').toLowerCase()})` });
      if (b.resolvedAt) events.push({ ts: b.resolvedAt, text: `${b.tradeRef} resolved${b.resolutionNote ? `: ${b.resolutionNote}` : ''}` });
    }
    return events.sort((a, c) => new Date(c.ts) - new Date(a.ts)).slice(0, 8);
  }, [breaks]);

  const kpis = useMemo(() => {
    const total = job?.tradesProcessed ?? 0;
    const breaksDetected = job?.breaksDetected ?? 0;
    const matched = total - breaksDetected;
    const openBreaks = breaks.filter((b) => b.status === 'OPEN').length;
    const highPriorityOpen = breaks.filter((b) => b.priority === 'HIGH' && b.status === 'OPEN').length;
    const resolvedToday = breaks.filter((b) => b.resolvedAt && new Date(b.resolvedAt).toDateString() === new Date().toDateString()).length;
    return {
      total,
      matched,
      matchRate: total ? ((matched / total) * 100).toFixed(0) : null,
      openBreaks,
      highPriorityOpen,
      resolvedToday,
    };
  }, [job, breaks]);

  const renderRow = useCallback((b) => (
    <>
      <span>{b.tradeRef}</span>
      <span>{b.instrumentSymbol}</span>
      <span>{b.counterpartyName}</span>
      <span>{Number(b.quantity).toLocaleString(undefined, { maximumFractionDigits: 4 })}</span>
      <span>{b.discrepancyType.replaceAll('_', ' ')}</span>
      <span><span className={`status-pill ${b.priority.toLowerCase()}`}>{b.priority}</span></span>
      <span><span className={`status-pill ${b.status.toLowerCase()}`}>{b.status}</span></span>
      <span>{new Date(b.detectedAt).toLocaleString()}</span>
      <span className="action-cell">
        {b.status === 'OPEN' && (
          <button className="btn-secondary" disabled={resolvingId === b.id} onClick={() => resolveBreak(b.id)}>
            Resolve
          </button>
        )}
      </span>
    </>
  ), [resolvingId]);

  return (
    <section>
      <PageTopbar title="Trade Reconciliation" subtitle="Compare internal trades against counterparty records and investigate exceptions." />

      {job && (
        <div className="kpi-grid">
          <article className="stat-card"><h3>Total Trades</h3><p>{kpis.total}</p>
            <span className="stat-card__meta">Processed this run</span>
          </article>
          <article className="stat-card"><h3>Matched</h3><p>{kpis.matched}</p>
            {kpis.matchRate !== null && <span className="stat-card__meta">{kpis.matchRate}% match rate</span>}
          </article>
          <article className="stat-card"><h3>Open Breaks</h3><p>{kpis.openBreaks}</p>
            {kpis.openBreaks > 0 && <span className="stat-card__meta stat-card__meta--warn">Requires attention</span>}
          </article>
          <article className="stat-card"><h3>High Priority</h3><p>{kpis.highPriorityOpen}</p>
            {kpis.highPriorityOpen > 0 && <span className="stat-card__meta stat-card__meta--warn">Cancelled trades</span>}
          </article>
          <article className="stat-card"><h3>Resolved Today</h3><p>{kpis.resolvedToday}</p></article>
        </div>
      )}

      <form onSubmit={runRecon} className="recon-form">
        <label>From <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label>To   <input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
        <button type="submit" className="btn-primary">Run recon</button>
        {jobId && (
          <>
            <span className="form-info">Job <code>{jobId}</code></span>
            <button type="button" className="btn-secondary" onClick={loadResults} disabled={loading}>
              {loading ? 'Loading…' : 'Load results'}
            </button>
          </>
        )}
      </form>

      {error && <div role="alert" className="form-error">{error}</div>}

      {job && (
        <>
          <div className="toolbar-card">
            <div className="toolbar-left">
              <input
                type="text"
                aria-label="Search by Trade ID"
                placeholder="Search Trade ID…"
                value={search}
                onChange={(e) => { setPage(0); setSearch(e.target.value); }}
              />
              <select aria-label="Filter by status" value={statusFilter} onChange={(e) => { setPage(0); setStatusFilter(e.target.value); }}>
                <option value="">All Status</option>
                <option value="OPEN">Open</option>
                <option value="RESOLVED">Resolved</option>
              </select>
              <select aria-label="Filter by priority" value={priorityFilter} onChange={(e) => { setPage(0); setPriorityFilter(e.target.value); }}>
                <option value="">All Priority</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
              <select aria-label="Filter by counterparty" value={counterpartyFilter} onChange={(e) => { setPage(0); setCounterpartyFilter(e.target.value); }}>
                <option value="">All Counterparties</option>
                {counterparties.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="toolbar-right">
              <button className="btn-secondary" onClick={loadResults} disabled={loading}>Refresh</button>
            </div>
          </div>

          {visibleBreaks.length > 0 ? (
            <DataTable sort={sortKey} onSortChange={handleSortChange} columnCount={COLUMNS.length}>
              <DataTable.Header columns={COLUMNS} />
              <DataTable.Body rows={pagedBreaks} render={renderRow} />
              <DataTable.Pagination page={page} totalPages={totalPages} onChange={setPage} />
            </DataTable>
          ) : (
            <p className="form-info">
              {breaks.length === 0
                ? 'No breaks in this run — every processed trade reconciled cleanly.'
                : 'No breaks match the current filters.'}
            </p>
          )}

          <div className="chart-card">
            <h3>Recent Activity</h3>
            {recentActivity.length === 0 ? (
              <p className="form-info">No activity yet for this run.</p>
            ) : (
              <table className="info-table">
                <tbody>
                  {recentActivity.map((ev, i) => (
                    <tr key={i}>
                      <td style={{ whiteSpace: 'nowrap' }}>{new Date(ev.ts).toLocaleTimeString()}</td>
                      <td>{ev.text}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {jobId && !job && <p className="form-info">Click &ldquo;Load results&rdquo; to fetch this run&apos;s stats and breaks.</p>}
    </section>
  );
}

export default withAuth(Recon);
