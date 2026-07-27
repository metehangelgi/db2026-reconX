package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        FXTrade trade = sampleFx("EUR", "USD");

        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("FXT-20260603-0001"));
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.FX);
        assertThat(trade.notional().currency()).isEqualTo(java.util.Currency.getInstance("USD"));
        assertThat(trade.notional().amount()).isEqualByComparingTo("110000");
    }

    @Test
    void builder_badIsoCode_throwsImmediately() {
        assertThatThrownBy(() -> FXTrade.builder().ccy1("EURR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builder_equalCurrencies_throwsOnBuild() {
        assertThatThrownBy(() -> sampleFx("EUR", "EUR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ccy1 and ccy2 must differ");
    }

    @Test
    void builder_nonPositiveFxRate_throwsOnBuild() {
        assertThatThrownBy(() -> FXTrade.builder()
                .tradeRef(TradeRef.of("FXT-20260603-0001"))
                .ccy1("EUR").ccy2("USD")
                .notionalCcy1(new BigDecimal("100000"))
                .fxRate(BigDecimal.ZERO)
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fxRate must be > 0");
    }

    @Test
    void builder_missingTradeRef_throws() {
        assertThatThrownBy(() -> FXTrade.builder()
                .ccy1("EUR").ccy2("USD")
                .notionalCcy1(new BigDecimal("100000"))
                .fxRate(new BigDecimal("1.1"))
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tradeRef");
    }

    private FXTrade sampleFx(String ccy1, String ccy2) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of("FXT-20260603-0001"))
                .ccy1(ccy1).ccy2(ccy2)
                .notionalCcy1(new BigDecimal("100000"))
                .fxRate(new BigDecimal("1.1"))
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
