package com.dbtraining.reconx.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReconExceptionTest {

    @Test
    void tradeNotFoundException_containsTradeRefInMessage() {
        TradeNotFoundException exception = new TradeNotFoundException("ABC-123");

        assertThat(exception)
                .isInstanceOf(ReconException.class)
                .hasMessage("Trade not found: ABC-123");
    }

    @Test
    void duplicateTradeRefException_containsTradeRefInMessage() {
        DuplicateTradeRefException exception = new DuplicateTradeRefException("ABC-123");

        assertThat(exception)
                .isInstanceOf(ReconException.class)
                .hasMessage("Duplicate tradeRef: ABC-123");
    }

    @Test
    void invalidTradeException_preservesMessage() {
        InvalidTradeException exception = new InvalidTradeException("trade is missing price");

        assertThat(exception)
                .isInstanceOf(ReconException.class)
                .hasMessage("trade is missing price");
    }

    @Test
    void reconciliationMismatchException_preservesMessage() {
        ReconciliationMismatchException exception = new ReconciliationMismatchException("mismatch between internal and external data");

        assertThat(exception)
                .isInstanceOf(ReconException.class)
                .hasMessage("mismatch between internal and external data");
    }

    @Test
    void reconException_canWrapCause() {
        IllegalStateException cause = new IllegalStateException("root cause");
        ReconException exception = new ReconException("wrapped", cause) {
        };

        assertThat(exception)
                .hasMessage("wrapped")
                .hasCause(cause);
    }
}
