package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV022 — DerivativeTrade with Builder pattern
 *
 * WHAT:    Option/derivative trade — underlying, strike, expiry, optionType.
 * HOW:     Same builder pattern. notional() = strike * quantity in the
 *          trade's currency (simplified — real derivatives use delta-adjusted).
 * ============================================================================
 */
public final class DerivativeTrade extends Trade implements TradeType {

    public enum OptionType { CALL, PUT }

    private final String underlying;
    private final BigDecimal strike;
    private final BigDecimal quantity;
    private final LocalDate expiry;
    private final OptionType optionType;
    private final Currency currency;
    private final Side side;
    private final long counterpartyId;

    /** Simplified notional = strike * quantity in the trade currency. */
    private DerivativeTrade(Builder b) {
        super(b.tradeRef, new Money(b.strike.multiply(b.quantity), b.currency), b.tradeDate);
        this.underlying     = b.underlying;
        this.strike         = b.strike;
        this.quantity       = b.quantity;
        this.expiry         = b.expiry;
        this.optionType     = b.optionType;
        this.currency       = b.currency;
        this.side           = b.side;
        this.counterpartyId = b.counterpartyId;
    }

    public static Builder builder() { return new Builder(); }

    @Override public AssetClass assetClass() { return AssetClass.DERIVATIVE; }

    public String underlying()       { return underlying; }
    public BigDecimal strike()       { return strike; }
    public BigDecimal quantity()     { return quantity; }
    public LocalDate expiry()        { return expiry; }
    public OptionType optionType()   { return optionType; }
    public Currency currency()       { return currency; }
    public Side side()               { return side; }
    public long counterpartyId()     { return counterpartyId; }

    @Override public boolean equals(Object o) {
        return (o instanceof DerivativeTrade other) && tradeRef().equals(other.tradeRef());
    }
    @Override public int hashCode() { return tradeRef().hashCode(); }

    @Override public String toString() {
        // NOTE: counterpartyId is deliberately omitted — it is the PII line in
        // this codebase and must never reach plain-text logs.
        return "DerivativeTrade[ref=%s, %s %s on %s, strike=%s %s, qty=%s, expiry=%s, side=%s]"
                .formatted(tradeRef(), optionType, underlying, tradeDate(),
                        strike.toPlainString(), currency.getCurrencyCode(),
                        quantity.toPlainString(), expiry, side);
    }

    public static final class Builder {
        private TradeRef tradeRef;
        private String underlying;
        private BigDecimal strike, quantity;
        private LocalDate expiry, tradeDate;
        private OptionType optionType;
        private Currency currency;
        private Side side;
        private long counterpartyId;

        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        public Builder underlying(String v)        { this.underlying = v; return this; }
        public Builder strike(BigDecimal v)        { this.strike = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v; return this; }
        public Builder expiry(LocalDate v)         { this.expiry = v; return this; }
        public Builder optionType(OptionType v)    { this.optionType = v; return this; }
        public Builder currency(String code)       { this.currency = Currency.getInstance(code); return this; }
        public Builder side(Side v)                { this.side = v; return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        public Builder counterpartyId(long v)      { this.counterpartyId = v; return this; }

        /**
         * Builds the immutable {@link DerivativeTrade}, validating that every
         * required field is set and that all invariants hold.
         *
         * <p>Note: {@code expiry} is validated only against {@code tradeDate} —
         * an {@code expiry} in the past relative to today is deliberately
         * accepted, since an expired derivative is still a valid historical
         * record.
         *
         * @return a fully-constructed, validated {@code DerivativeTrade} — never {@code null}
         * @throws NullPointerException  if any required field ({@code tradeRef},
         *                               {@code underlying}, {@code strike},
         *                               {@code quantity}, {@code expiry},
         *                               {@code optionType}, {@code currency},
         *                               {@code side}, {@code tradeDate}) was not set
         * @throws IllegalStateException if {@code strike} or {@code quantity} is not
         *                               strictly positive, or {@code expiry} is not
         *                               strictly after {@code tradeDate}
         */
        public DerivativeTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef is required");
            Objects.requireNonNull(underlying, "underlying is required");
            Objects.requireNonNull(strike, "strike is required");
            Objects.requireNonNull(quantity, "quantity is required");
            Objects.requireNonNull(expiry, "expiry is required");
            Objects.requireNonNull(optionType, "optionType is required");
            Objects.requireNonNull(currency, "currency is required");
            Objects.requireNonNull(side, "side is required");
            Objects.requireNonNull(tradeDate, "tradeDate is required");

            if (strike.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("strike must be > 0");
            }
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }
            // An option struck and expiring on the same day has zero time value
            // left to trade, so expiry must be strictly after tradeDate.
            if (!expiry.isAfter(tradeDate)) {
                throw new IllegalStateException("expiry must be strictly after tradeDate");
            }

            // Deliberately NOT checked here: expiry relative to LocalDate.now().
            // A derivative whose expiry has already passed by "today" is still a
            // valid historical record (e.g. backfilled or closed-out trades), so
            // it must remain constructible — only its relation to tradeDate is
            // validated above.

            return new DerivativeTrade(this);
        }
    }
}
