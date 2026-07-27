package com.dbtraining.reconx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.service.TradeAnalyticsService.NotionalSummary;

class TradeAnalyticsServiceTest {

    @Test
    void test() {
        List<? extends TradeType> trades = List.of(
                EquityTrade.builder()
                        .tradeRef(TradeRef.of("ABC-20260727-1234"))
                        .instrumentSymbol("SAP.DE")
                        .quantity(new BigDecimal("100"))
                        .price(new BigDecimal("100"))
                        .currency("EUR").side(Side.BUY)
                        .tradeDate(LocalDate.of(2026, 6, 3))
                        .counterpartyId(1L)
                        .build()
        );

        TradeAnalyticsService service = new TradeAnalyticsService();
        Map<Long, NotionalSummary> map = service.notionalByCounterparty(trades);
        assertThat(map).hasSize(1);
        assertThat(map.get(1L)).isEqualTo(1);

    }
}