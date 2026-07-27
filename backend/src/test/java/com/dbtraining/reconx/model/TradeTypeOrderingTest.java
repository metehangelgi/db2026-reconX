package com.dbtraining.reconx.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class TradeTypeOrderingTest {

    @Test
    void heterogeneousTreeSet_ordersNewestFirstAndUsesTradeRefTiebreaker() {
        TradeType newest = equityTrade("EQU-20260603-0002", LocalDate.of(2026, 6, 3));
        TradeType middle = derivativeTrade("DVT-20260602-0001", LocalDate.of(2026, 6, 2));
        TradeType oldest = equityTrade("EQU-20260601-0001", LocalDate.of(2026, 6, 1));
        TradeType derivative = derivativeTrade("DVT-20260603-0001", LocalDate.of(2026, 6, 3));

        TreeSet<TradeType> trades = new TreeSet<>();
        trades.add(newest);
        trades.add(middle);
        trades.add(oldest);
        trades.add(derivative);

        assertThat(trades)
                .containsExactly(newest, derivative, middle, oldest);

        assertThat(newest.compareTo(derivative)).isGreaterThan(0);
        assertThat(derivative.compareTo(newest)).isLessThan(0);
        assertThat(newest.compareTo(newest)).isZero();
    }

    private TradeType equityTrade(String ref, LocalDate tradeDate) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(1L)
                .build();
    }

    private TradeType fxTrade(String ref, LocalDate tradeDate) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .ccy1("EUR")
                .ccy2("USD")
                .notionalCcy1(new BigDecimal("1000"))
                .fxRate(new BigDecimal("1.1"))
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(2L)
                .build();
    }

    private TradeType bondTrade(String ref, LocalDate tradeDate) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .isin("US0000000001")
                .faceValue(new BigDecimal("1000"))
                .couponRate(new BigDecimal("0.05"))
                .maturityDate(tradeDate.plusDays(30))
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(3L)
                .build();
    }

    private TradeType derivativeTrade(String ref, LocalDate tradeDate) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .underlying("AAPL")
                .strike(new BigDecimal("10"))
                .quantity(new BigDecimal("100"))
                .expiry(tradeDate.plusDays(30))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(4L)
                .build();
    }
}
