package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV044 — Testcontainers-managed PostgreSQL wired into the Spring test context.
 * TICKET-ADV045 — end-to-end insert -> recon -> verify assertions.
 */
@SpringBootTest
@Testcontainers
class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconResultRepository reconResultRepo;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Test
    void containerIsRunning() {
        // sanity: if this passes, all your wiring is correct.
        // The real assertions live in TICKET-ADV045.
    }

    @Test
    void insertedTradesAreReconciledAndPersisted() {
        // given — a matching pair of trades (constructed directly: this schema has a
        // single UNIQUE trade_ref trades table, not separate internal/external books)
        EquityTrade internal = equity("EQU-20260603-9001", "245.50", "100");
        EquityTrade external = equity("EQU-20260603-9001", "245.50", "100");

        // when
        reconciliationService.runRecon(
                List.<TradeType>of(internal), List.<TradeType>of(external), ReconciliationRule.EXACT);

        // then — exactly one MATCHED row landed in recon_results
        List<ReconResult> persisted = reconResultRepo.findAll();
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("EQU-20260603-9001");
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
