package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BondTradeTest {

    @Test
    void shouldBuildValidBondTrade() {
        BondTrade trade = BondTrade.builder()
                .tradeRef(TradeRef.of("BND-20260727-0001"))
                .isin("GB00B03MLX29")
                .faceValue(new BigDecimal("1000000.00"))
                .couponRate(new BigDecimal("0.045"))
                .maturityDate(LocalDate.of(2035, 7, 27))
                .currency("GBP")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 27))
                .counterpartyId(1L)
                .build();

        assertEquals(TradeType.AssetClass.BOND, trade.assetClass());
        assertEquals("GB00B03MLX29", trade.isin());
        assertEquals(new BigDecimal("1000000.00"), trade.faceValue());
    }

    @Test
    void shouldRejectMaturityDateBeforeTradeDate() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validBuilder()
                        .tradeDate(LocalDate.of(2026, 7, 27))
                        .maturityDate(LocalDate.of(2025, 7, 27))
                        .build()
        );

        assertEquals(
                "maturityDate must be strictly after tradeDate",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectMaturityDateEqualToTradeDate() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validBuilder()
                        .tradeDate(LocalDate.of(2026, 7, 27))
                        .maturityDate(LocalDate.of(2026, 7, 27))
                        .build()
        );

        assertEquals(
                "maturityDate must be strictly after tradeDate",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectIsinWithWrongLength() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validBuilder()
                        .isin("SHORT")
                        .build()
        );

        assertEquals(
                "ISIN must be exactly 12 characters",
                exception.getMessage()
        );
    }

    private BondTrade.Builder validBuilder() {
        return BondTrade.builder()
                .tradeRef(TradeRef.of("BND-20260727-0001"))
                .isin("GB00B03MLX29")
                .faceValue(new BigDecimal("1000000.00"))
                .couponRate(new BigDecimal("0.045"))
                .maturityDate(LocalDate.of(2035, 7, 27))
                .currency("GBP")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 27))
                .counterpartyId(1L);
    }
}