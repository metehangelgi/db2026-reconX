package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV030 — hand-written, PII-safe toString() on every concrete trade.
 */
class TradeToStringTest {

    @Test
    void equityTrade_toString_excludesCounterpartyId_includesCommercialFields() {
        EquityTrade trade = equity(42L);

        String s = trade.toString();

        assertThat(s).doesNotContain(String.valueOf(trade.counterpartyId()));
        assertThat(s).contains("ABC-20260727-1232", "SAP.DE", "100", "EUR", "BUY");
    }

    @Test
    void fxTrade_toString_excludesCounterpartyId_includesCommercialFields() {
        FXTrade trade = fx(42L);

        String s = trade.toString();

        assertThat(s).doesNotContain(String.valueOf(trade.counterpartyId()));
        assertThat(s).contains("FXT-20260603-0001", "EUR", "USD", "100000", "1.1", "BUY");
    }

    @Test
    void bondTrade_toString_excludesCounterpartyId_includesCommercialFields() {
        BondTrade trade = bond(42L);

        String s = trade.toString();

        assertThat(s).doesNotContain(String.valueOf(trade.counterpartyId()));
        assertThat(s).contains("BND-20260603-0001", "US1234567890", "1000", "USD", "5", "BUY");
    }

    @Test
    void derivativeTrade_toString_excludesCounterpartyId_includesCommercialFields() {
        DerivativeTrade trade = derivative(42L);

        String s = trade.toString();

        assertThat(s).doesNotContain(String.valueOf(trade.counterpartyId()));
        assertThat(s).contains("DRV-20260603-0001", "CALL", "AAPL", "150", "USD", "10", "BUY");
    }

    @Test
    void bigDecimalFields_renderPlain_neverScientificNotation() {
        FXTrade trade = FXTrade.builder()
                .tradeRef(TradeRef.of("FXT-20260603-0002"))
                .ccy1("EUR").ccy2("USD")
                .notionalCcy1(new BigDecimal("100000000"))
                .fxRate(new BigDecimal("0.000001"))
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();

        String s = trade.toString();

        assertThat(s).doesNotContain("E+").doesNotContain("E-");
        assertThat(s).contains("100000000", "0.000001");
    }

    private EquityTrade equity(long counterpartyId) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-1232"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(counterpartyId)
                .build();
    }

    private FXTrade fx(long counterpartyId) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of("FXT-20260603-0001"))
                .ccy1("EUR").ccy2("USD")
                .notionalCcy1(new BigDecimal("100000"))
                .fxRate(new BigDecimal("1.1"))
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(counterpartyId)
                .build();
    }

    private BondTrade bond(long counterpartyId) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of("BND-20260603-0001"))
                .isin("US1234567890")
                .faceValue(new BigDecimal("1000"))
                .couponRate(new BigDecimal("5"))
                .maturityDate(LocalDate.of(2030, 6, 3))
                .currency("USD").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(counterpartyId)
                .build();
    }

    private DerivativeTrade derivative(long counterpartyId) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of("DRV-20260603-0001"))
                .underlying("AAPL")
                .strike(new BigDecimal("150"))
                .quantity(new BigDecimal("10"))
                .expiry(LocalDate.of(2026, 12, 18))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("USD").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(counterpartyId)
                .build();
    }
}
