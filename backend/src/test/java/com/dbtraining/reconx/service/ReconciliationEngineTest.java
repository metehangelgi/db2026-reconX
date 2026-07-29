package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV040 / ADV041 / ADV042 — TDD: write the test FIRST, then the impl.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    @DisplayName("exact match on price and qty returns MATCHED")
    void testReconcile_exactMatch_returnsMatched() {
        // given
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // when
        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(external), ReconciliationRule.EXACT);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance -> MATCHED")
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    void testReconcile_priceTolerance_withinThreshold(String diff) {
        BigDecimal basePrice = new BigDecimal("100.00");
        EquityTrade internal = equity("EQU-20260603-0002", basePrice.toPlainString(), "1000");
        EquityTrade external = equity("EQU-20260603-0002", basePrice.add(new BigDecimal(diff)).toPlainString(), "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT);

        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("internal trade with no external counterpart returns BREAK with reason MISSING_EXTERNAL")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // given
        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        // when
        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(), ReconciliationRule.EXACT);

        // then
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    @DisplayName("empty internal and external lists return an empty result list")
    void testReconcile_emptyInternal_returnsEmpty() {
        // given: no internal trades and no external trades

        // when
        List<ReconResult> out = engine.reconcile(List.of(), List.of(), ReconciliationRule.EXACT);

        // then
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("all-mismatched trades: ReconSummary reports zero matched, all broken")
    void testReconcile_allMismatched_summaryShowsZeroMatched() {
        // given
        List<TradeType> internals = List.<TradeType>of(
                equity("EQU-20260603-9101", "100.00", "1000"),
                equity("EQU-20260603-9102", "100.00", "1000"),
                equity("EQU-20260603-9103", "100.00", "1000"));
        List<TradeType> externals = List.<TradeType>of(
                equity("EQU-20260603-9101", "200.00", "1000"),
                equity("EQU-20260603-9102", "200.00", "1000"),
                equity("EQU-20260603-9103", "200.00", "1000"));

        // when
        List<ReconResult> out = engine.reconcile(internals, externals, ReconciliationRule.EXACT);
        ReconSummary summary = out.stream().collect(new ReconSummaryCollector());

        // then
        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(3);
    }

    @Test
    void reconcileByCounterparty_mergesAllPerCounterpartyResults() {
        Map<Long, List<TradeType>> internalByCp = Map.of(
                1L, List.<TradeType>of(equity("EQU-20260603-0001", "100.00", "100"),
                        equity("EQU-20260603-0002", "100.00", "100")),
                2L, List.<TradeType>of(equity("EQU-20260603-0003", "100.00", "100"))
        );
        Map<Long, List<TradeType>> externalByCp = Map.of(
                1L, List.<TradeType>of(equity("EQU-20260603-0001", "100.00", "100"),
                        equity("EQU-20260603-0002", "100.00", "100")),
                2L, List.<TradeType>of(equity("EQU-20260603-0003", "100.00", "100"))
        );

        List<ReconResult> out = engine.reconcileByCounterparty(
                internalByCp, externalByCp, ReconciliationRule.EXACT).join();

        assertThat(out).hasSize(3);
        assertThat(out).allMatch(r -> r.status() == ReconResult.Status.MATCHED);

        engine.shutdown();
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
