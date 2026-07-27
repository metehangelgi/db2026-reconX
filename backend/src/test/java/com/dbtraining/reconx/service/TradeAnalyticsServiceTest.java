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

    private EquityTrade equityTrade(String symbol, BigDecimal quantity, BigDecimal price) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-" + (1000 + symbol.length())))
                .instrumentSymbol(symbol)
                .quantity(quantity)
                .price(price)
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 27))
                .counterpartyId(1L)
                .build();
    }
}
