package com.dbtraining.reconx.exception;

/** TICKET-ADV025 — 409 Conflict: tradeRef already exists. */
public class DuplicateTradeRefException extends ReconException {
    /** @param tradeRef the reference string that already exists */
    public DuplicateTradeRefException(String tradeRef) {
        super("Duplicate tradeRef: " + tradeRef);
    }

    /**
     * Preserves the original failure (e.g. a unique-constraint violation) as the cause.
     *
     * @param tradeRef the reference string that already exists
     * @param cause    the underlying failure that triggered this exception
     */
    public DuplicateTradeRefException(String tradeRef, Throwable cause) {
        super("Duplicate tradeRef: " + tradeRef, cause);
    }
}
