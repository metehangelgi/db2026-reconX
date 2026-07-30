package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * Immutable value object: Money
 *
 * WHAT:    Record bundling a {@link BigDecimal} amount with a {@link Currency}.
 *          Used everywhere a monetary value crosses a boundary (DTO, event,
 *          metric).
 * HOW:     Compact constructor enforces: non-null amount, non-null currency,
 *          non-negative amount. {@link BigDecimal} (not double) prevents
 *          accumulating floating-point error on aggregations.
 * WHY:     Passing raw BigDecimal around loses currency context — a USD 100
 *          can be silently added to a EUR 100. Money makes the mismatch
 *          fail at the type level: {@code plus()} throws if currencies differ.
 * OBSERVE: {@code Money.of("100.00","USD").plus(Money.of("50","EUR"))} throws.
 *          {@code Money.of("100","USD").plus(Money.of("50","USD"))} returns 150 USD.
 * ============================================================================
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }
    }

    /**
     * Parses a decimal string amount and an ISO-4217 currency code into a {@link Money}.
     *
     * @param amount       a valid {@link BigDecimal} literal, non-negative
     * @param currencyCode a valid ISO-4217 code (e.g. {@code "USD"})
     * @return a new {@code Money} instance
     * @throws NumberFormatException    if {@code amount} is not a valid decimal literal
     * @throws IllegalArgumentException if {@code currencyCode} is not a recognised ISO-4217 code,
     *                                   or the resulting amount is negative
     */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Pairs an already-parsed amount with an ISO-4217 currency code.
     *
     * @param amount       a non-negative amount
     * @param currencyCode a valid ISO-4217 code (e.g. {@code "USD"})
     * @return a new {@code Money} instance
     * @throws IllegalArgumentException if {@code currencyCode} is not a recognised ISO-4217 code,
     *                                   or {@code amount} is negative
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Adds another {@code Money} of the same currency.
     *
     * @param other the amount to add; must share this instance's currency
     * @return a new {@code Money} holding the sum, in the shared currency
     * @throws IllegalArgumentException if {@code other}'s currency differs from this one's
     */
    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add %s to %s — currency mismatch".formatted(other.currency, this.currency));
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Scales this amount by {@code multiplier}, keeping the same currency.
     *
     * @param multiplier the scaling factor; may be negative or fractional
     * @return a new {@code Money} holding {@code amount * multiplier} in this currency
     * @throws IllegalArgumentException if the result is negative (enforced by the compact constructor)
     */
    public Money times(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
}