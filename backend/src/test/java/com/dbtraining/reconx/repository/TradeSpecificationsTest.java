package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV056 — Specification-based dynamic queries: verifies each factory
 * short-circuits correctly and that composed specs narrow results as expected.
 */
@SpringBootTest
@Testcontainers
class TradeSpecificationsTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private CounterpartyRepository counterpartyRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Test
    void onlyDateRangeSupplied_returnsTradesInsideRange() {
        Counterparty cp1 = counterpartyRepository.findById(1L).orElseThrow();
        Counterparty cp2 = counterpartyRepository.findById(2L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade inRange = save("SPEC-20260710-0001", instrument, cp1, LocalDate.of(2026, 7, 10), TradeStatus.PENDING);
        Trade outOfRange = save("SPEC-20261001-0002", instrument, cp2, LocalDate.of(2026, 10, 1), TradeStatus.PENDING);

        Specification<Trade> spec = tradeDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
                .and(hasStatus(null))
                .and(hasCounterparty(null));

        Page<Trade> page = tradeRepository.findAll(spec, PageRequest.of(0, 1000));

        assertThat(page.getContent())
                .extracting(Trade::getTradeRef)
                .contains(inRange.getTradeRef())
                .doesNotContain(outOfRange.getTradeRef());
    }

    @Test
    void allFourFiltersSupplied_narrowsToExactMatch() {
        Counterparty cp1 = counterpartyRepository.findById(3L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade match = save("SPEC-20260712-0003", instrument, cp1, LocalDate.of(2026, 7, 12), TradeStatus.CONFIRMED);
        Trade wrongStatus = save("SPEC-20260712-0004", instrument, cp1, LocalDate.of(2026, 7, 12), TradeStatus.PENDING);

        Specification<Trade> spec = tradeDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
                .and(hasStatus("CONFIRMED"))
                .and(hasCounterparty(cp1.getId()))
                .and(refLike("SPEC-20260712"));

        Page<Trade> page = tradeRepository.findAll(spec, PageRequest.of(0, 50));

        assertThat(page.getContent())
                .extracting(Trade::getTradeRef)
                .containsExactly(match.getTradeRef());
        assertThat(page.getContent())
                .extracting(Trade::getTradeRef)
                .doesNotContain(wrongStatus.getTradeRef());
    }

    @Test
    void hasAssetClass_narrowsToMatchingAssetClassOnly() {
        Counterparty cp1 = counterpartyRepository.findById(4L).orElseThrow();
        Instrument equity = instrumentRepository.findById(1L).orElseThrow();
        Instrument bond = instrumentRepository.findById(10L).orElseThrow();

        Trade equityTrade = save("SPEC-20260713-0005", equity, cp1, LocalDate.of(2026, 7, 13), TradeStatus.PENDING);
        Trade bondTrade = save("SPEC-20260713-0006", bond, cp1, LocalDate.of(2026, 7, 13), TradeStatus.PENDING);

        Specification<Trade> spec = hasAssetClass("EQUITY");
        Page<Trade> page = tradeRepository.findAll(spec, PageRequest.of(0, 1000));

        assertThat(page.getContent()).extracting(Trade::getTradeRef).contains(equityTrade.getTradeRef());
        assertThat(page.getContent()).extracting(Trade::getTradeRef).doesNotContain(bondTrade.getTradeRef());
    }

    @Test
    void hasAssetClass_nullOrBlank_matchesEverything() {
        Specification<Trade> spec = hasAssetClass(null);
        // conjunction() means "no constraint" — just verify it composes without throwing
        // and returns at least the seeded 500 trades.
        Page<Trade> page = tradeRepository.findAll(spec, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isGreaterThan(0);
    }

    private Trade save(String ref, Instrument instrument, Counterparty counterparty,
                        LocalDate tradeDate, TradeStatus status) {
        Trade trade = new Trade();
        trade.setTradeRef(ref);
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass(instrument.getAssetClass().name());
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("100.0000"));
        trade.setPrice(new BigDecimal("50.0000"));
        trade.setTradeDate(tradeDate);
        trade.setStatus(status);
        return tradeRepository.save(trade);
    }
}
