package com.dbtraining.reconx.exception;

/** TICKET-ADV025 — 400 Bad Request: a trade failed business validation. */
public class InvalidTradeException extends ReconException {
    /** @param message a human-readable description of which validation failed */
    public InvalidTradeException(String message) { super(message); }

    /**
     * Preserves the original failure (e.g. a parse/cast exception) as the cause.
     *
     * @param message a human-readable description of which validation failed
     * @param cause   the underlying failure that triggered this exception
     */
    public InvalidTradeException(String message, Throwable cause) { super(message, cause); }
}
