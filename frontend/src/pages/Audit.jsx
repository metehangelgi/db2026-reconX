// TICKET-ADV071 (frontend) — audit trail lookup by tradeRef.
import React, { useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import PageTopbar from '@components/PageTopbar.jsx';
import { api } from '@services/apiService.js';

function Audit() {
  const [tradeRef, setTradeRef] = useState('');
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function search(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.audit(tradeRef.trim());
      setEntries(res);
    } catch (err) {
      setError(err.message);
      setEntries(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <PageTopbar title="Reports & Audit Trail" subtitle="Look up the full event history for a single trade." />

      {entries && (
        <div className="kpi-grid">
          <article className="stat-card"><h3>Events Found</h3><p>{entries.length}</p></article>
        </div>
      )}

      <form onSubmit={search} className="audit-form">
        <label>
          Trade ref
          <input
            value={tradeRef}
            onChange={(e) => setTradeRef(e.target.value)}
            placeholder="TST-20260729-0001"
            required
          />
        </label>
        <button disabled={loading} type="submit">Search</button>
      </form>

      {error && <div role="alert" className="form-error">{error}</div>}

      {entries && entries.length === 0 && <p>No audit history yet for this trade.</p>}

      {entries && entries.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Event type</th>
                <th>Timestamp</th>
                <th>Actor</th>
                <th>Before</th>
                <th>After</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <tr key={e.id}>
                  <td>{e.eventType}</td>
                  <td>{e.eventTimestamp}</td>
                  <td>{e.actor}</td>
                  <td>{e.beforeState ?? '—'}</td>
                  <td>{e.afterState ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default withAuth(Audit);
