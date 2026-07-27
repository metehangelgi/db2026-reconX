package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.dbtraining.reconx.model.TradeType.AssetClass;

class EquityTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        // TODO(TICKET-ADV019): build an EquityTrade via the Builder with all required fields,
        //                     then assert tradeRef, notional (price*qty) and assetClass = EQUITY.
        EquityTrade trade = EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-1232"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();

        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("ABC-20260727-1232"));
        assertThat(trade.notional().amount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(trade.assetClass()).isEqualTo(AssetClass.EQUITY);
    }

    @Test
    void builder_missingPrice_throws() {
        // TODO(TICKET-ADV019): omit .price(...) on the Builder and assert build() throws
        //                     NullPointerException whose message mentions "price".
        assertThatThrownBy(() -> EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260727-1232"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("price");
    }

    @Test
    void equality_byTradeRef() {
        EquityTrade t1 = sampleEquity("ABC-20260727-1232");
        EquityTrade t2 = sampleEquity("ABC-20260727-1232");
        EquityTrade t3 = sampleEquity("ABC-20260727-9999");

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
        assertThat(t1).isNotEqualTo(t3);
    }

    private EquityTrade sampleEquity(String ref) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L).build();
    }
}
