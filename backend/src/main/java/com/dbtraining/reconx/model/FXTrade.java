package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV020 — FXTrade with Builder pattern
 *
 * WHAT:    FX spot/forward trade — two currencies, a deal amount in ccy1
 *          ({@code notionalCcy1}), and an fxRate.
 * HOW:     Same builder pattern as EquityTrade. {@link #notional()} rolls the
 *          deal amount up into ccy2 (the quote currency) via fxRate, so
 *          reconciliation summaries across mixed FX pairs are comparable.
 * WHY:     FX has two natural sides — a EUR/USD trade is BOTH a buy of EUR
 *          AND a sell of USD. Modelling that with two distinct currency
 *          fields makes settlement-side reasoning explicit.
 * OBSERVE: notional().currency() == ccy2; .amount() == notionalCcy1 * fxRate.
 * ============================================================================
 */
public final class FXTrade extends Trade implements TradeType {

    private final Currency ccy1;
    private final Currency ccy2;
    private final BigDecimal notionalCcy1;
    private final BigDecimal fxRate;
    private final Side side;
    private final long counterpartyId;

    /** Deal amount is in ccy1; the reconciliation-facing notional rolls that up into ccy2 = notionalCcy1 * fxRate. */
    private FXTrade(Builder b) {
        super(b.tradeRef, new Money(b.notionalCcy1.multiply(b.fxRate), b.ccy2), b.tradeDate);
        this.ccy1           = b.ccy1;
        this.ccy2           = b.ccy2;
        this.notionalCcy1   = b.notionalCcy1;
        this.fxRate         = b.fxRate;
        this.side           = b.side;
        this.counterpartyId = b.counterpartyId;
    }

    public static Builder builder() { return new Builder(); }

    @Override public AssetClass assetClass() { return AssetClass.FX; }

    public Currency ccy1()           { return ccy1; }
    public Currency ccy2()           { return ccy2; }
    public BigDecimal notionalCcy1() { return notionalCcy1; }
    public BigDecimal fxRate()       { return fxRate; }
    public Side side()               { return side; }
    public long counterpartyId()     { return counterpartyId; }

    @Override public boolean equals(Object o) {
        return (o instanceof FXTrade other) && tradeRef().equals(other.tradeRef());
    }
    @Override public int hashCode() { return tradeRef().hashCode(); }

    @Override public String toString() {
        // NOTE: counterpartyId is deliberately omitted — it is the PII line in
        // this codebase and must never reach plain-text logs.
        return "FXTrade[ref=%s, %s/%s, notional=%s %s, rate=%s, side=%s]"
                .formatted(tradeRef(), ccy1.getCurrencyCode(), ccy2.getCurrencyCode(),
                        notionalCcy1.toPlainString(), ccy1.getCurrencyCode(),
                        fxRate.toPlainString(), side);
    }

    public static final class Builder {
        private TradeRef tradeRef;
        private Currency ccy1, ccy2;
        private BigDecimal notionalCcy1, fxRate;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        public Builder ccy1(String code)           { this.ccy1 = Currency.getInstance(code); return this; }
        public Builder ccy2(String code)           { this.ccy2 = Currency.getInstance(code); return this; }
        public Builder notionalCcy1(BigDecimal v)  { this.notionalCcy1 = v; return this; }
        public Builder fxRate(BigDecimal v)        { this.fxRate = v; return this; }
        public Builder side(Side v)                { this.side = v; return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        public Builder counterpartyId(long v)      { this.counterpartyId = v; return this; }

        /**
         * Builds the immutable {@link FXTrade}, validating that every required
         * field is set and that all invariants hold.
         *
         * @return a fully-constructed, validated {@code FXTrade} — never {@code null}
         * @throws NullPointerException  if any required field ({@code tradeRef},
         *                               {@code ccy1}, {@code ccy2}, {@code notionalCcy1},
         *                               {@code fxRate}, {@code side}, {@code tradeDate})
         *                               was not set
         * @throws IllegalStateException if {@code ccy1} equals {@code ccy2}, or
         *                               {@code fxRate}/{@code notionalCcy1} is not
         *                               strictly positive
         */
        public FXTrade build() {
            Objects.requireNonNull(tradeRef,     "tradeRef");
            Objects.requireNonNull(ccy1,         "ccy1");
            Objects.requireNonNull(ccy2,         "ccy2");
            Objects.requireNonNull(notionalCcy1, "notionalCcy1");
            Objects.requireNonNull(fxRate,       "fxRate");
            Objects.requireNonNull(side,         "side");
            Objects.requireNonNull(tradeDate,    "tradeDate");
            if (ccy1.equals(ccy2)) {
                throw new IllegalStateException("ccy1 and ccy2 must differ");
            }
            if (fxRate.signum() <= 0) {
                throw new IllegalStateException("fxRate must be > 0");
            }
            if (notionalCcy1.signum() <= 0) {
                throw new IllegalStateException("notionalCcy1 must be > 0");
            }
            return new FXTrade(this);
        }
    }
}
