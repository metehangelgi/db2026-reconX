import React, { useState } from 'react';
import { api } from '@services/apiService.js';

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'FX', 'COMMODITY', 'DERIVATIVE'];
const SIDES = ['BUY', 'SELL'];

function ViewBody({ trade }) {
  const rows = [
    ['Trade ID', trade.tradeRef],
    ['ISIN', trade.isin || '—'],
    ['Instrument', trade.instrumentSymbol],
    ['Counterparty', trade.counterpartyName],
    ['Asset Class', trade.assetClass],
    ['Side', trade.side],
    ['Quantity', trade.quantity],
    ['Price', trade.price],
    ['Trade Value', (trade.quantity * trade.price).toLocaleString(undefined, { maximumFractionDigits: 2 })],
    ['Trade Date', trade.tradeDate],
    ['Status', trade.status],
    ['Created', trade.createdAt ? new Date(trade.createdAt).toLocaleString() : '—'],
  ];
  return (
    <dl className="detail-grid">
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

function EditBody({ trade, onClose, onSaved }) {
  const [form, setForm] = useState({
    instrumentId: trade.instrumentId,
    counterpartyId: trade.counterpartyId,
    assetClass: trade.assetClass,
    side: trade.side,
    quantity: trade.quantity,
    price: trade.price,
    tradeDate: trade.tradeDate,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      // tradeRef is intentionally not sent as editable — TradeService.update()
      // never applies it, the trade keeps its original reference regardless.
      await api.updateTrade(trade.id, {
        tradeRef: trade.tradeRef,
        instrumentId: Number(form.instrumentId),
        counterpartyId: Number(form.counterpartyId),
        assetClass: form.assetClass,
        side: form.side,
        quantity: Number(form.quantity),
        price: Number(form.price),
        tradeDate: form.tradeDate,
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="modal-form" onSubmit={submit}>
      <label>
        Trade ID (not editable)
        <input value={trade.tradeRef} disabled />
      </label>
      <label>
        Instrument ID
        <input type="number" value={form.instrumentId} onChange={(e) => set('instrumentId', e.target.value)} required />
      </label>
      <label>
        Counterparty ID
        <input type="number" value={form.counterpartyId} onChange={(e) => set('counterpartyId', e.target.value)} required />
      </label>
      <label>
        Asset class
        <select value={form.assetClass} onChange={(e) => set('assetClass', e.target.value)}>
          {ASSET_CLASSES.map((a) => <option key={a} value={a}>{a}</option>)}
        </select>
      </label>
      <label>
        Side
        <select value={form.side} onChange={(e) => set('side', e.target.value)}>
          {SIDES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </label>
      <label>
        Quantity
        <input type="number" step="any" value={form.quantity} onChange={(e) => set('quantity', e.target.value)} required />
      </label>
      <label>
        Price
        <input type="number" step="any" value={form.price} onChange={(e) => set('price', e.target.value)} required />
      </label>
      <label>
        Trade date
        <input type="date" value={form.tradeDate} onChange={(e) => set('tradeDate', e.target.value)} required />
      </label>

      {error && <div role="alert" className="form-error">{error}</div>}

      <div className="modal-form__actions">
        <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
        <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
      </div>
    </form>
  );
}

export default function TradeModal({ trade, mode, onClose, onSaved }) {
  if (!trade) return null;
  return (
    <div className="modal-scrim" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={`${mode === 'edit' ? 'Edit' : 'View'} trade ${trade.tradeRef}`}>
        <div className="modal-card__header">
          <h3>{mode === 'edit' ? 'Edit Trade' : 'Trade Details'} — {trade.tradeRef}</h3>
          <button className="modal-card__close" onClick={onClose} aria-label="Close">×</button>
        </div>
        {mode === 'edit'
          ? <EditBody trade={trade} onClose={onClose} onSaved={onSaved} />
          : <ViewBody trade={trade} />}
      </div>
    </div>
  );
}
