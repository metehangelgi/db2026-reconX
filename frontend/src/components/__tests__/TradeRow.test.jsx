// TICKET-ADV119 / TICKET-ADV121 — RTL test for the memoised <TradeRow>.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { TradeRow } from '../TradeRow.jsx';

const trade = {
  id: 42,
  tradeRef: 'TRD-0042',
  instrumentSymbol: 'UST-10Y',
  quantity: 1000,
  price: 99.75,
  status: 'MATCHED',
};

describe('<TradeRow>', () => {
  it("renders the trade's fields", () => {
    render(
      <table>
        <tbody>
          <TradeRow trade={trade} onClick={() => {}} />
        </tbody>
      </table>
    );

    expect(screen.getByText('TRD-0042')).toBeInTheDocument();
    expect(screen.getByText('UST-10Y')).toBeInTheDocument();
    expect(screen.getByText('1000')).toBeInTheDocument();
    expect(screen.getByText('99.75')).toBeInTheDocument();
    expect(screen.getByText('MATCHED')).toBeInTheDocument();
  });

  it('calls onClick with the trade id when the row is clicked', async () => {
    const onClick = vi.fn();
    render(
      <table>
        <tbody>
          <TradeRow trade={trade} onClick={onClick} />
        </tbody>
      </table>
    );

    await userEvent.click(screen.getByText('TRD-0042'));

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onClick).toHaveBeenCalledWith(42);
  });
});
