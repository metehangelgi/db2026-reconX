package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DerivativeTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        DerivativeTrade trade = validBuilder().build();

        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.DERIVATIVE);
        assertThat(trade.notional().amount()).isEqualByComparingTo("1500");
    }

    @Test
    void builder_expiryEqualToTradeDate_throwsOnBuild() {
        assertThatThrownBy(() -> validBuilder()
                .tradeDate(LocalDate.of(2026, 6, 3))
                .expiry(LocalDate.of(2026, 6, 3))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expiry must be strictly after tradeDate");
    }

    @Test
    void builder_expiryBeforeTradeDate_throwsOnBuild() {
        assertThatThrownBy(() -> validBuilder()
                .tradeDate(LocalDate.of(2026, 6, 3))
                .expiry(LocalDate.of(2026, 6, 2))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expiry must be strictly after tradeDate");
    }

    @Test
    void builder_expiryInThePastRelativeToToday_isStillAValidHistoricalRecord() {
        // An expired derivative is a valid historical record and must not be
        // rejected just because "today" is now after its expiry.
        DerivativeTrade trade = validBuilder()
                .tradeDate(LocalDate.of(2020, 1, 1))
                .expiry(LocalDate.of(2020, 6, 1))
                .build();

        assertThat(trade.expiry()).isEqualTo(LocalDate.of(2020, 6, 1));
    }

    @Test
    void builder_nonPositiveStrike_throwsOnBuild() {
        assertThatThrownBy(() -> validBuilder().strike(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("strike must be > 0");
    }

    @Test
    void builder_nonPositiveQuantity_throwsOnBuild() {
        assertThatThrownBy(() -> validBuilder().quantity(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("quantity must be > 0");
    }

    private DerivativeTrade.Builder validBuilder() {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of("DRV-20260603-0001"))
                .underlying("AAPL")
                .strike(new BigDecimal("150"))
                .quantity(new BigDecimal("10"))
                .expiry(LocalDate.of(2026, 12, 18))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L);
    }
}
