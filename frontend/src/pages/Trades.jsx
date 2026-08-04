// TICKET-ADV114 — Compound DataTable; sort state lives here and is wired
// through onSortChange.
// TICKET-ADV117 — useDebouncedSearch.
import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import PageTopbar from '@components/PageTopbar.jsx';
import TradeModal from '@components/TradeModal.jsx';
import { useAuth } from '@context/AuthContext.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'FX', 'COMMODITY', 'DERIVATIVE'];

const COLUMNS = [
  { key: 'tradeRef',     label: 'Trade ID' },
  { key: 'isin',         label: 'ISIN' },
  { key: 'symbol',       label: 'Instrument' },
  { key: 'counterparty', label: 'Counterparty' },
  { key: 'qty',          label: 'Quantity' },
  { key: 'price',        label: 'Price' },
  { key: 'value',        label: 'Trade Value' },
  { key: 'status',       label: 'Status' },
  { key: 'action',       label: '' },
];

const COLUMN_TO_FIELD = {
  tradeRef: 'tradeRef',
  isin: 'isin',
  symbol: 'instrumentSymbol',
  counterparty: 'counterpartyName',
  qty: 'quantity',
  price: 'price',
  value: 'tradeValue',
  status: 'status',
};

function toCsvValue(v) {
  const s = String(v ?? '');
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

function downloadCsv(filename, rows, columns) {
  const header = columns.map((c) => c.label).join(',');
  const lines = rows.map((r) => columns.map((c) => toCsvValue(c.value(r))).join(','));
  const blob = new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function Trades() {
  const { user } = useAuth();
  const canDelete = user?.role === 'ADMIN';
  const canEdit = user?.role === 'TRADER' || user?.role === 'ADMIN';
  const navigate = useNavigate();
  const importInputRef = useRef(null);

  const [statusFilter, setStatusFilter] = useState('');
  const [assetClassFilter, setAssetClassFilter] = useState('');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');
  const [deletingId, setDeletingId] = useState(null);
  const [modal, setModal] = useState(null); // { mode: 'view'|'edit', trade }
  const [importSummary, setImportSummary] = useState(null);

  // A separate, larger, unfiltered fetch backs the "Trade Value" KPI so it
  // at least samples the whole book rather than just the current paginated/
  // filtered page — though past `size=1000` rows it's a sample, not the true
  // sum, since summing 90k+ rows in the browser isn't reasonable. Counts
  // (below) don't have this problem: Spring Data's totalElements comes from
  // a COUNT query, so `?status=X&size=1` gets an exact count for ~nothing.
  const [allTrades, setAllTrades] = useState([]);
  const loadAllTrades = useCallback(() => {
    api.listTrades('?size=1000').then((res) => setAllTrades(res.items)).catch(() => setAllTrades([]));
  }, []);
  useEffect(loadAllTrades, [loadAllTrades]);

  const [statusCounts, setStatusCounts] = useState({ total: 0, confirmed: 0, settled: 0, pending: 0, cancelled: 0 });
  const loadStatusCounts = useCallback(() => {
    Promise.all([
      api.listTrades('?size=1'),
      api.listTrades('?status=CONFIRMED&size=1'),
      api.listTrades('?status=SETTLED&size=1'),
      api.listTrades('?status=PENDING&size=1'),
      api.listTrades('?status=CANCELLED&size=1'),
    ]).then(([total, confirmed, settled, pending, cancelled]) => {
      setStatusCounts({
        total: total.totalElements,
        confirmed: confirmed.totalElements,
        settled: settled.totalElements,
        pending: pending.totalElements,
        cancelled: cancelled.totalElements,
      });
    }).catch(() => setStatusCounts({ total: 0, confirmed: 0, settled: 0, pending: 0, cancelled: 0 }));
  }, []);
  useEffect(loadStatusCounts, [loadStatusCounts]);

  const loadPage = useCallback(() => {
    const params = new URLSearchParams({ page: String(page) });
    if (statusFilter) params.set('status', statusFilter);
    if (assetClassFilter) params.set('assetClass', assetClassFilter);
    api.listTrades(`?${params.toString()}`)
      .then((res) => setData(res))
      .catch(() => setData({ items: [], totalPages: 0 }));
  }, [page, statusFilter, assetClassFilter]);
  useEffect(loadPage, [loadPage]);

  const refreshAll = useCallback(() => {
    loadPage();
    loadAllTrades();
    loadStatusCounts();
  }, [loadPage, loadAllTrades, loadStatusCounts]);

  const handleSortChange = useCallback((key) => {
    setSortDir((prevDir) => (sortKey === key ? (prevDir === 'asc' ? 'desc' : 'asc') : 'asc'));
    setSortKey(key);
  }, [sortKey]);

  const handleDelete = useCallback(async (id) => {
    if (!window.confirm('Delete this trade? This cannot be undone from here.')) return;
    setDeletingId(id);
    try {
      await api.deleteTrade(id);
      refreshAll();
    } catch (err) {
      window.alert(err.message);
    } finally {
      setDeletingId(null);
    }
  }, [refreshAll]);

  function handleExport() {
    downloadCsv('trades-export.csv', visibleItems, [
      { label: 'Trade ID', value: (t) => t.tradeRef },
      { label: 'ISIN', value: (t) => t.isin },
      { label: 'Instrument', value: (t) => t.instrumentSymbol },
      { label: 'Counterparty', value: (t) => t.counterpartyName },
      { label: 'Quantity', value: (t) => t.quantity },
      { label: 'Price', value: (t) => t.price },
      { label: 'Trade Value', value: (t) => t.tradeValue },
      { label: 'Status', value: (t) => t.status },
    ]);
  }

  async function handleImportFile(e) {
    const file = e.target.files?.[0];
    e.target.value = ''; // allow re-selecting the same file next time
    if (!file) return;

    const text = await file.text();
    const [headerLine, ...rows] = text.trim().split(/\r?\n/);
    const headers = headerLine.split(',').map((h) => h.trim());
    let succeeded = 0;
    const failures = [];

    for (const line of rows) {
      if (!line.trim()) continue;
      const cells = line.split(',').map((c) => c.trim());
      const record = Object.fromEntries(headers.map((h, i) => [h, cells[i]]));
      try {
        await api.createTrade({
          tradeRef: record.tradeRef,
          instrumentId: Number(record.instrumentId),
          counterpartyId: Number(record.counterpartyId),
          assetClass: record.assetClass,
          side: record.side,
          quantity: Number(record.quantity),
          price: Number(record.price),
          tradeDate: record.tradeDate,
        });
        succeeded++;
      } catch (err) {
        failures.push(`${record.tradeRef || line}: ${err.message}`);
      }
    }

    setImportSummary({ succeeded, failures });
    refreshAll();
  }

  // Free-text search only reaches as far as the currently-loaded page — the
  // REST API has no tradeRef/instrument/counterparty search parameter today,
  // so this can't be a true server-side search without a backend change.
  const visibleItems = useMemo(() => {
    let items = data.items.map((t) => ({ ...t, tradeValue: t.quantity * t.price }));
    if (debouncedSearch) {
      const needle = debouncedSearch.toLowerCase();
      items = items.filter((t) =>
        t.tradeRef.toLowerCase().includes(needle) ||
        (t.instrumentSymbol || '').toLowerCase().includes(needle) ||
        (t.isin || '').toLowerCase().includes(needle) ||
        (t.counterpartyName || '').toLowerCase().includes(needle)
      );
    }
    if (!sortKey) return items;
    const field = COLUMN_TO_FIELD[sortKey];
    if (!field) return items;
    const dir = sortDir === 'asc' ? 1 : -1;
    return [...items].sort((a, b) => {
      const av = a[field];
      const bv = b[field];
      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
      return String(av).localeCompare(String(bv)) * dir;
    });
  }, [data.items, debouncedSearch, sortKey, sortDir]);

  const renderRow = useCallback((row) => (
    <>
      <span>{row.tradeRef}</span>
      <span>{row.isin || '—'}</span>
      <span>{row.instrumentSymbol}</span>
      <span>{row.counterpartyName}</span>
      <span>{row.quantity}</span>
      <span>{row.price}</span>
      <span>{row.tradeValue.toLocaleString(undefined, { maximumFractionDigits: 2 })}</span>
      <span><span className={`status-pill ${row.status.toLowerCase()}`}>{row.status}</span></span>
      <span className="action-cell">
        <button className="btn-secondary" onClick={() => setModal({ mode: 'view', trade: row })}>View</button>
        {canEdit && (
          <button className="btn-secondary" onClick={() => setModal({ mode: 'edit', trade: row })}>Edit</button>
        )}
        {canDelete && (
          <button
            className="btn-secondary"
            disabled={deletingId === row.id}
            onClick={() => handleDelete(row.id)}
          >
            Delete
          </button>
        )}
      </span>
    </>
  ), [canDelete, canEdit, deletingId, handleDelete]);

  const tradeValueSample = useMemo(
    () => allTrades.reduce((sum, t) => sum + t.quantity * t.price, 0),
    [allTrades]
  );

  const kpis = useMemo(() => ({
    total:   statusCounts.total,
    matched: statusCounts.confirmed + statusCounts.settled,
    pending: statusCounts.pending,
    breaks:  statusCounts.cancelled,
    value:   tradeValueSample,
  }), [statusCounts, tradeValueSample]);

  return (
    <section>
      <PageTopbar title="Trading Dashboard" subtitle="Front office trade blotter" />

      <div className="kpi-grid">
        <article className="stat-card"><h3>Total Trades</h3><p>{kpis.total.toLocaleString()}</p></article>
        <article className="stat-card"><h3>Matched</h3><p>{kpis.matched.toLocaleString()}</p>
          <span className="stat-card__meta">{kpis.total ? `${((kpis.matched / kpis.total) * 100).toFixed(0)}%` : '—'}</span>
        </article>
        <article className="stat-card"><h3>Pending</h3><p>{kpis.pending.toLocaleString()}</p></article>
        <article className="stat-card"><h3>Breaks</h3><p>{kpis.breaks.toLocaleString()}</p>
          {kpis.breaks > 0 && <span className="stat-card__meta stat-card__meta--warn">Needs investigation</span>}
        </article>
        <article className="stat-card"><h3>Trade Value</h3><p>{kpis.value.toLocaleString(undefined, { maximumFractionDigits: 0 })}</p>
          {kpis.total > allTrades.length && (
            <span className="stat-card__meta">first {allTrades.length.toLocaleString()} of {kpis.total.toLocaleString()} trades</span>
          )}
        </article>
      </div>

      <div className="toolbar-card">
        <div className="toolbar-left">
          <input
            type="text"
            aria-label="Search trades on this page"
            placeholder="Search Trade ID, ISIN, Instrument or Counterparty…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select aria-label="Filter by status" value={statusFilter} onChange={(e) => { setPage(0); setStatusFilter(e.target.value); }}>
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="SETTLED">Settled</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <select aria-label="Filter by instrument type" value={assetClassFilter} onChange={(e) => { setPage(0); setAssetClassFilter(e.target.value); }}>
            <option value="">All Instruments</option>
            {ASSET_CLASSES.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </div>
        <div className="toolbar-right">
          <button className="btn-primary" onClick={() => navigate('/trades/new')}>+ New Trade</button>
          <button className="btn-secondary" onClick={() => importInputRef.current?.click()}>Import</button>
          <input ref={importInputRef} type="file" accept=".csv" hidden onChange={handleImportFile} />
          <button className="btn-secondary" onClick={handleExport}>Export</button>
          <button className="btn-secondary" onClick={refreshAll}>Refresh</button>
        </div>
      </div>

      {importSummary && (
        <p className="form-info">
          Import finished: {importSummary.succeeded} created
          {importSummary.failures.length > 0 && `, ${importSummary.failures.length} failed (${importSummary.failures.slice(0, 3).join('; ')}${importSummary.failures.length > 3 ? '…' : ''})`}.
          {' '}CSV columns expected: tradeRef,instrumentId,counterpartyId,assetClass,side,quantity,price,tradeDate.
        </p>
      )}

      <DataTable sort={sortKey} onSortChange={handleSortChange} columnCount={COLUMNS.length}>
        <DataTable.Header columns={COLUMNS} />
        <DataTable.Body
          rows={visibleItems}
          render={renderRow}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>

      {modal && (
        <TradeModal
          trade={modal.trade}
          mode={modal.mode}
          onClose={() => setModal(null)}
          onSaved={refreshAll}
        />
      )}
    </section>
  );
}

export default withAuth(Trades);
