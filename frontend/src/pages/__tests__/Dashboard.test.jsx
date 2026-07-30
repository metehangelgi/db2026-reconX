// TICKET-ADV125 — RTL test for the dashboard summary cards.
import { screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders } from '../../test-utils/renderWithProviders.jsx';

const trades = [
  { id: 1, tradeRef: 'TRD-0001', quantity: 100, price: 99.5, status: 'PENDING', tradeDate: '2026-01-01', counterpartyName: 'Acme' },
  { id: 2, tradeRef: 'TRD-0002', quantity: 200, price: 101.25, status: 'CONFIRMED', tradeDate: '2026-01-01', counterpartyName: 'Acme' },
  { id: 3, tradeRef: 'TRD-0003', quantity: 50, price: 100.0, status: 'SETTLED', tradeDate: '2026-01-01', counterpartyName: 'Beta' },
  { id: 4, tradeRef: 'TRD-0004', quantity: 10, price: 98.75, status: 'CANCELLED', tradeDate: '2026-01-01', counterpartyName: 'Beta' },
  { id: 5, tradeRef: 'TRD-0005', quantity: 75, price: 102.0, status: 'CANCELLED', tradeDate: '2026-01-01', counterpartyName: 'Gamma' },
];

vi.mock('@hooks/useTradeStream.js', () => ({
  useTradeStream: () => ({ trades, isConnected: true }),
}));

// Dashboard also loads pre-existing trades on mount (so it isn't blank until
// something streams in) — stub that out to isolate this test to the SSE data.
vi.mock('@services/apiService.js', () => ({
  api: {
    listTrades: () => Promise.resolve({ items: [] }),
    health: () => Promise.resolve({ components: { reconxDatabase: { status: 'UP' }, kafka: { status: 'UP' } } }),
  },
}));

// jsdom has no real <canvas> 2D context, so Chart.js can't actually render in
// this environment — these tests only assert on the KPI cards, not the
// charts, so stub the chart components out entirely.
vi.mock('react-chartjs-2', () => ({
  Line: () => null,
  Doughnut: () => null,
  Bar: () => null,
}));

import Dashboard from '../Dashboard.jsx';

describe('<Dashboard>', () => {
  it('renders all four summary stat cards', async () => {
    await act(async () => renderWithProviders(<Dashboard />));

    expect(screen.getByRole('heading', { name: /trade value/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /total trades/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /matched/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /open breaks/i })).toBeInTheDocument();
  });

  it('computes trade value, matched and open-breaks counts from the streamed trades', async () => {
    await act(async () => renderWithProviders(<Dashboard />));

    // trade value = sum(quantity * price) for all trades, rendered with locale grouping
    const expectedTradeValue = trades
      .reduce((sum, t) => sum + t.quantity * t.price, 0)
      .toLocaleString(undefined, { maximumFractionDigits: 0 });

    const valueHeading = screen.getByRole('heading', { name: /trade value/i });
    expect(valueHeading.closest('article')).toHaveTextContent(expectedTradeValue);

    const totalHeading = screen.getByRole('heading', { name: /total trades/i });
    expect(totalHeading.closest('article')).toHaveTextContent(String(trades.length));

    // matched = CONFIRMED + SETTLED = 2
    const matchedHeading = screen.getByRole('heading', { name: /matched/i });
    expect(matchedHeading.closest('article')).toHaveTextContent('2');

    // breaks = CANCELLED = 2
    const breaksHeading = screen.getByRole('heading', { name: /open breaks/i });
    expect(breaksHeading.closest('article')).toHaveTextContent('2');
  });
});
