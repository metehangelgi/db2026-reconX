package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV018 — Internal shared state for every {@link TradeType}
 *
 * Package-private: only the four sealed {@link TradeType} leaves may extend
 * this class. It exists purely to centralise the three fields every trade has
 * in common (tradeRef, notional, tradeDate) and validate them once, instead
 * of repeating the same three {@code Objects.requireNonNull} calls in four
 * builders. The public contract remains {@link TradeType}; this class is not
 * part of it.
 * ============================================================================
 */
abstract sealed class Trade permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    private final TradeRef tradeRef;
    private final Money notional;
    private final LocalDate tradeDate;

    protected Trade(TradeRef tradeRef, Money notional, LocalDate tradeDate) {
        this.tradeRef = Objects.requireNonNull(tradeRef, "tradeRef");
        this.notional = Objects.requireNonNull(notional, "notional");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate");
    }

    public final TradeRef tradeRef()   { return tradeRef; }
    public final Money notional()      { return notional; }
    public final LocalDate tradeDate() { return tradeDate; }
}
