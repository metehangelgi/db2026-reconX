package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TradeAnalyticsServiceTest {

    private final TradeAnalyticsService service = new TradeAnalyticsService();

    @Test
    void notionalByCounterparty_countsAndSumsNotionalPerCounterparty() {
        List<EquityTrade> trades = List.of(
                equityTradeForCounterparty(1L, new BigDecimal("100"), new BigDecimal("10")),
                equityTradeForCounterparty(1L, new BigDecimal("200"), new BigDecimal("5")),
                equityTradeForCounterparty(2L, new BigDecimal("50"), new BigDecimal("4"))
        );

        Map<Long, TradeAnalyticsService.NotionalSummary> result = service.notionalByCounterparty(trades);

        assertThat(result.get(1L).count()).isEqualTo(2);
        assertThat(result.get(1L).total()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.get(2L).count()).isEqualTo(1);
        assertThat(result.get(2L).total()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    void vwapByInstrument_returnsWeightedAveragePerInstrument() {
        List<EquityTrade> trades = List.of(
                equityTrade("AAPL", new BigDecimal("100"), new BigDecimal("200")),
                equityTrade("AAPL", new BigDecimal("200"), new BigDecimal("100")),
                equityTrade("MSFT", new BigDecimal("50"), new BigDecimal("300"))
        );

        Map<String, BigDecimal> result = service.vwapByInstrument(trades);

        assertThat(result).containsEntry("AAPL", new BigDecimal("133.3333"));
        assertThat(result).containsEntry("MSFT", new BigDecimal("300.0000"));
    }

    @Test
    void vwapByInstrument_returnsEmptyMapForEmptyInput() {
        // TICKET-ADV035 — empty input should not throw and should return no rows.
        Map<String, BigDecimal> result = service.vwapByInstrument(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void pnlByInstrument_sumsBuyAndSellPnlPerInstrument() {
        List<EquityTrade> trades = List.of(
                equityTrade("AAPL", new BigDecimal("100"), new BigDecimal("200"), Side.BUY),
                equityTrade("AAPL", new BigDecimal("50"), new BigDecimal("250"), Side.SELL),
                equityTrade("MSFT", new BigDecimal("40"), new BigDecimal("300"), Side.BUY),
                equityTrade("GOOG", new BigDecimal("10"), new BigDecimal("1000"), Side.SELL),
                equityTrade("GOOG", new BigDecimal("10"), new BigDecimal("900"), Side.BUY)
        );

        Map<String, BigDecimal> result = service.pnlByInstrument(trades);

        assertThat(result.get("AAPL")).isEqualByComparingTo(new BigDecimal("-7500"));
        assertThat(result.get("MSFT")).isEqualByComparingTo(new BigDecimal("-12000"));
        assertThat(result.get("GOOG")).isEqualByComparingTo(new BigDecimal("1000"));
    }

    private EquityTrade equityTradeForCounterparty(long counterpartyId, BigDecimal price, BigDecimal quantity) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-" + (2000 + counterpartyId)))
                .instrumentSymbol("AAPL")
                .quantity(quantity)
                .price(price)
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 27))
                .counterpartyId(counterpartyId)
                .build();
    }

    private EquityTrade equityTrade(String symbol, BigDecimal quantity, BigDecimal price) {
        return equityTrade(symbol, quantity, price, Side.BUY);
    }

    private EquityTrade equityTrade(String symbol, BigDecimal quantity, BigDecimal price, Side side) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-" + (1000 + symbol.length())))
                .instrumentSymbol(symbol)
                .quantity(quantity)
                .price(price)
                .currency("USD")
                .side(side)
                .tradeDate(LocalDate.of(2026, 7, 27))
                .counterpartyId(1L)
                .build();
    }
}
