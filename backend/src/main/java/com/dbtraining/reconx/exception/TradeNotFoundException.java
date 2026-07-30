package com.dbtraining.reconx.exception;

/** TICKET-ADV025 — 404 Not Found: tradeRef has no row in trades. */
public class TradeNotFoundException extends ReconException {
    /** @param tradeRef the reference string that produced no matching trade */
    public TradeNotFoundException(String tradeRef) {
        super("Trade not found: " + tradeRef);
    }

    /**
     * Preserves the original lookup failure (e.g. a repository exception) as the cause.
     *
     * @param tradeRef the reference string that produced no matching trade
     * @param cause    the underlying failure that triggered this exception
     */
    public TradeNotFoundException(String tradeRef, Throwable cause) {
        super("Trade not found: " + tradeRef, cause);
    }
}
