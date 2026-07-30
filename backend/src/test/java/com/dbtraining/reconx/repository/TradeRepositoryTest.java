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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV055 — smoke test for TradeRepository.findByFilters against a real Postgres
 * instance (Testcontainers), exercising the seeded counterparties/instruments.
 */
@SpringBootTest
@Testcontainers
class TradeRepositoryTest {

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
    void findByFilters_withDateRangeOnly_returnsTradesInsideRange() {
        // given — one trade inside the target window, one outside it
        Counterparty counterparty = counterpartyRepository.findById(1L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade inside = newTrade("ADV-20260610-0001", instrument, counterparty,
                LocalDate.of(2026, 6, 10), TradeStatus.PENDING);
        Trade outside = newTrade("ADV-20260901-0002", instrument, counterparty,
                LocalDate.of(2026, 9, 1), TradeStatus.PENDING);
        tradeRepository.save(inside);
        tradeRepository.save(outside);

        // when
        Page<Trade> page = tradeRepository.findByFilters(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                null, null, PageRequest.of(0, 1000));

        // then
        assertThat(page.getContent())
                .extracting(Trade::getTradeRef)
                .contains("ADV-20260610-0001")
                .doesNotContain("ADV-20260901-0002");
    }

    @Test
    void findByFilters_withStatusAndCounterparty_narrowsResults() {
        Counterparty counterparty = counterpartyRepository.findById(2L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade confirmed = newTrade("ADV-20260615-0003", instrument, counterparty,
                LocalDate.of(2026, 6, 15), TradeStatus.CONFIRMED);
        Trade pending = newTrade("ADV-20260615-0004", instrument, counterparty,
                LocalDate.of(2026, 6, 15), TradeStatus.PENDING);
        tradeRepository.save(confirmed);
        tradeRepository.save(pending);

        Page<Trade> page = tradeRepository.findByFilters(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                TradeStatus.CONFIRMED, counterparty.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Trade::getTradeRef)
                .contains("ADV-20260615-0003")
                .doesNotContain("ADV-20260615-0004");
    }

    private Trade newTrade(String ref, Instrument instrument, Counterparty counterparty,
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
        return trade;
    }
}
