package com.dbtraining.reconx.model;

import com.dbtraining.reconx.exception.InvalidTradeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TICKET-ADV023 — TradeFactory must never let a raw ClassCastException,
 * NullPointerException, or IllegalArgumentException escape; every failure
 * surfaces as InvalidTradeException with a useful message.
 */
class TradeFactoryTest {

    private Map<String, Object> validEquityFields() {
        Map<String, Object> p = new HashMap<>();
        p.put("tradeRef", "EQU-20260603-0001");
        p.put("symbol", "SAP.DE");
        p.put("quantity", "100");
        p.put("price", "245.50");
        p.put("currency", "EUR");
        p.put("side", "BUY");
        p.put("tradeDate", "2026-06-03");
        p.put("counterpartyId", 1L);
        return p;
    }

    @Test
    void create_validEquityFields_returnsEquityTrade() {
        TradeType trade = TradeFactory.create("EQUITY", validEquityFields());

        assertThat(trade).isInstanceOf(EquityTrade.class);
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.EQUITY);
    }

    @Test
    void create_caseInsensitiveAssetClass_works() {
        TradeType trade = TradeFactory.create("equity", validEquityFields());
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.EQUITY);
    }

    @Test
    void create_unknownAssetClass_throwsInvalidTradeException() {
        assertThatThrownBy(() -> TradeFactory.create("CRYPTO", validEquityFields()))
                .isInstanceOf(InvalidTradeException.class)
                .hasNoSuppressedExceptions();
    }

    @Test
    void create_missingRequiredKey_throwsInvalidTradeExceptionNotNullPointerException() {
        Map<String, Object> fields = validEquityFields();
        fields.remove("price");

        assertThatThrownBy(() -> TradeFactory.create("EQUITY", fields))
                .isInstanceOf(InvalidTradeException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void create_wrongFieldType_throwsInvalidTradeExceptionNotClassCastException() {
        Map<String, Object> fields = validEquityFields();
        fields.put("counterpartyId", "not-a-number"); // expected Number, actual String

        assertThatThrownBy(() -> TradeFactory.create("EQUITY", fields))
                .isInstanceOf(InvalidTradeException.class)
                .hasCauseInstanceOf(ClassCastException.class);
    }

    @Test
    void create_builderRejectsData_wrapsIllegalStateExceptionAsInvalidTradeException() {
        Map<String, Object> fields = validEquityFields();
        fields.put("quantity", "-5"); // EquityTrade.Builder rejects non-positive quantity

        assertThatThrownBy(() -> TradeFactory.create("EQUITY", fields))
                .isInstanceOf(InvalidTradeException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_bondTrade_fromMap() {
        Map<String, Object> p = new HashMap<>();
        p.put("tradeRef", "BND-20260603-0001");
        p.put("isin", "US0378331005");
        p.put("faceValue", "1000");
        p.put("couponRate", "0.05");
        p.put("maturityDate", LocalDate.now().plusYears(1).toString());
        p.put("currency", "USD");
        p.put("side", "BUY");
        p.put("tradeDate", LocalDate.now().toString());
        p.put("counterpartyId", 2L);

        TradeType trade = TradeFactory.create("BOND", p);

        assertThat(trade).isInstanceOf(BondTrade.class);
        assertThat(((BondTrade) trade).faceValue()).isEqualByComparingTo(new BigDecimal("1000"));
    }
}
