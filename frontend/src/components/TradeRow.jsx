// frontend/src/components/TradeRow.jsx
// TICKET-ADV119 — React.memo with a custom equality check so rows only
// re-render when a field they actually display changes.
// TICKET-ADV121 — the onClick identity check here only holds if the parent
// wraps its handler in useCallback; see frontend/src/pages/Trades.jsx.
import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <tr onClick={() => onClick(trade.id)}>
      <td>{trade.tradeRef}</td>
      <td>{trade.instrumentSymbol}</td>
      <td>{trade.quantity}</td>
      <td>{trade.price}</td>
      <td><span className={`status-pill ${trade.status.toLowerCase()}`}>{trade.status}</span></td>
    </tr>
  );
}

// Custom equality — only the fields we actually render, not JSON.stringify.
function areEqual(prev, next) {
  return prev.trade.id      === next.trade.id
      && prev.trade.status  === next.trade.status
      && prev.trade.price   === next.trade.price
      && prev.onClick       === next.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
