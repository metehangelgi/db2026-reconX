package com.dbtraining.reconx.exception;

/** TICKET-ADV025 — 422 Unprocessable: internal vs external trade do not match. */
public class ReconciliationMismatchException extends ReconException {

    private final Long reconBreakId;

    /** @param message a human-readable description of the mismatch */
    public ReconciliationMismatchException(String message) {
        super(message);
        this.reconBreakId = null;
    }

    /**
     * Preserves the original failure as the cause.
     *
     * @param message a human-readable description of the mismatch
     * @param cause   the underlying failure that triggered this exception
     */
    public ReconciliationMismatchException(String message, Throwable cause) {
        super(message, cause);
        this.reconBreakId = null;
    }

    /**
     * @param message      a human-readable description of the mismatch
     * @param reconBreakId the id of the persisted recon break this mismatch corresponds to
     */
    public ReconciliationMismatchException(String message, Long reconBreakId) {
        super(message);
        this.reconBreakId = reconBreakId;
    }

    /** @return the id of the persisted recon break, or {@code null} if this exception was raised before one was persisted */
    public Long getReconBreakId() { return reconBreakId; }
}
