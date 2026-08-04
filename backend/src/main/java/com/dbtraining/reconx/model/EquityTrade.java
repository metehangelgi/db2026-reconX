package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV019 — EquityTrade with Builder pattern
 *
 * WHAT:    Concrete TradeType for equity (cash share) trades.
 * HOW:     Final class, all fields final, no setters. Construction is via the
 *          nested {@link Builder} which validates in {@link Builder#build()}.
 * WHY:     Eight required fields on a single constructor is unreadable at
 *          the call site. Builder gives named arguments, makes the validity
 *          check a single chokepoint, and the object stays immutable.
 * OBSERVE: Calling build() with a missing required field throws
 *          IllegalStateException — verified by EquityTradeTest.
 * HINT:    Same shape applied to FXTrade/BondTrade/DerivativeTrade.
 * ============================================================================
 *
 * TICKET-ADV028 — equals/hashCode from tradeRef (Object methods on a regular class)
 * TICKET-ADV030 — toString() omits PII, prints reference/symbol/qty/price/side
 */
public final class EquityTrade extends Trade implements TradeType {

    private final String instrumentSymbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final Currency currency;
    private final Side side;
    private final long counterpartyId;

    /** Notional = quantity * price in the trade currency. */
    private EquityTrade(Builder b) {
        super(b.tradeRef, new Money(b.quantity.multiply(b.price), b.currency), b.tradeDate);
        this.instrumentSymbol = b.instrumentSymbol;
        this.quantity         = b.quantity;
        this.price            = b.price;
        this.currency         = b.currency;
        this.side             = b.side;
        this.counterpartyId   = b.counterpartyId;
    }

    public static Builder builder() { return new Builder(); }

    @Override public AssetClass assetClass(){ return AssetClass.EQUITY; }

    public String instrumentSymbol() { return instrumentSymbol; }
    public BigDecimal quantity()     { return quantity; }
    public BigDecimal price()        { return price; }
    public Currency currency()       { return currency; }
    public Side side()               { return side; }
    public long counterpartyId()     { return counterpartyId; }

    /** equals: two EquityTrades are equal iff their tradeRef is equal. */
    @Override
    public boolean equals(Object o) {
        return (o instanceof EquityTrade other) && tradeRef().equals(other.tradeRef());
    }

    @Override public int hashCode() { return tradeRef().hashCode(); }

    @Override
    public String toString() {
        // NOTE: counterpartyId is deliberately omitted — it is the PII line in
        // this codebase and must never reach plain-text logs.
        return "EquityTrade[ref=%s, symbol=%s, qty=%s, price=%s %s, side=%s]"
                .formatted(tradeRef(), instrumentSymbol, quantity.toPlainString(),
                        price.toPlainString(), currency.getCurrencyCode(), side);
    }

    /** Fluent builder. Required fields validated in {@link #build()}. */
    public static final class Builder {
        private TradeRef tradeRef;
        private String instrumentSymbol;
        private BigDecimal quantity;
        private BigDecimal price;
        private Currency currency;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef v)           { this.tradeRef = v;        return this; }
        public Builder instrumentSymbol(String v)     { this.instrumentSymbol = v; return this; }
        public Builder quantity(BigDecimal v)         { this.quantity = v;        return this; }
        public Builder price(BigDecimal v)            { this.price = v;           return this; }
        public Builder currency(Currency v)           { this.currency = v;        return this; }
        public Builder currency(String code)          { return currency(Currency.getInstance(code)); }
        public Builder side(Side v)                   { this.side = v;            return this; }
        public Builder tradeDate(LocalDate v)         { this.tradeDate = v;       return this; }
        public Builder counterpartyId(long v)         { this.counterpartyId = v;  return this; }

        /**
         * Builds the immutable {@link EquityTrade}, validating that every required
         * field is set and that all invariants hold.
         *
         * @return a fully-constructed, validated {@code EquityTrade} — never {@code null}
         * @throws NullPointerException  if any required field ({@code tradeRef},
         *                               {@code instrumentSymbol}, {@code quantity},
         *                               {@code price}, {@code currency}, {@code side},
         *                               {@code tradeDate}) was not set
         * @throws IllegalStateException if {@code quantity} or {@code price} is not
         *                               strictly positive
         */
        public EquityTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef is required");
            Objects.requireNonNull(instrumentSymbol, "instrumentSymbol is required");
            Objects.requireNonNull(quantity, "quantity is required");
            Objects.requireNonNull(price, "price is required");
            Objects.requireNonNull(currency, "currency is required");
            Objects.requireNonNull(side, "side is required");
            Objects.requireNonNull(tradeDate, "tradeDate is required");

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("price must be > 0");
            }

            return new EquityTrade(this);
        }
    }
}
