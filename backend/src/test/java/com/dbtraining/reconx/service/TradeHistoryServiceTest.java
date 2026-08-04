package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.InstrumentAssetClass;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV052 — Hibernate Envers writes one trades_aud row per committed
 * change. Not annotated @Transactional: each repository save below must
 * commit independently (Spring Data's save() carries its own @Transactional),
 * otherwise Envers would only see one revision for the whole test.
 */
@SpringBootTest
class TradeHistoryServiceTest {

    @Autowired private TradeRepository tradeRepo;
    @Autowired private CounterpartyRepository counterpartyRepo;
    @Autowired private InstrumentRepository instrumentRepo;
    @Autowired private TradeHistoryService historyService;

    @Test
    void threeUpdates_produceFourRevisions() {
        Counterparty cp = new Counterparty();
        cp.setName("Acme");
        cp.setLeiCode("LEI" + System.nanoTime());
        cp.setRegion("EMEA");
        cp = counterpartyRepo.save(cp);

        Instrument instrument = new Instrument();
        instrument.setSymbol("HIST" + System.nanoTime() % 100000);
        instrument.setName("History Test Instrument");
        instrument.setAssetClass(InstrumentAssetClass.EQUITY);
        instrument.setCurrency("EUR");
        instrument = instrumentRepo.save(instrument);

        Trade trade = new Trade();
        trade.setTradeRef("HIST-" + System.nanoTime());
        trade.setCounterparty(cp);
        trade.setInstrument(instrument);
        trade.setAssetClass("EQUITY");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("100"));
        trade.setPrice(new BigDecimal("50.00"));
        trade.setTradeDate(LocalDate.now());
        trade.setStatus(TradeStatus.PENDING);
        trade = tradeRepo.save(trade); // revision 1: insert

        trade.setStatus(TradeStatus.CONFIRMED);
        trade = tradeRepo.save(trade); // revision 2: update

        trade.setStatus(TradeStatus.SETTLED);
        trade = tradeRepo.save(trade); // revision 3: update

        trade.setPrice(new BigDecimal("51.00"));
        trade = tradeRepo.save(trade); // revision 4: update

        List<Number> revisions = historyService.revisionsFor(trade.getId());

        assertThat(revisions).hasSize(4);

        Trade firstRevision = historyService.snapshotAt(trade.getId(), revisions.get(0));
        assertThat(firstRevision.getStatus()).isEqualTo(TradeStatus.PENDING);

        Trade lastRevision = historyService.snapshotAt(trade.getId(), revisions.get(3));
        assertThat(lastRevision.getStatus()).isEqualTo(TradeStatus.SETTLED);
        assertThat(lastRevision.getPrice()).isEqualByComparingTo("51.00");
    }
}
