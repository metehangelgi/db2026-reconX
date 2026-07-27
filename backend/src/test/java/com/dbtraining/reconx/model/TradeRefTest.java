package com.dbtraining.reconx.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TradeRefTest {

    @Test
    void traderef_success() {
        TradeRef tradeRef = TradeRef.of("EQU-20260602-0001");
        assertThat(tradeRef.toString()).isEqualTo("EQU-20260602-0001");
    }

    @Test
    void traderef_error() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> TradeRef.of("foo"));
    }

    @Test
    void traderef_error_null() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> TradeRef.of(null));
    }
}
