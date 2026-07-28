package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeLookupServiceTest {

    @Test
    void resolvesCounterpartyForKnownTradeRef() {
        TradeRepository tradeRepo = mock(TradeRepository.class);
        CounterpartyRepository cpRepo = mock(CounterpartyRepository.class);
        TradeLookupService service = new TradeLookupService(tradeRepo, cpRepo);

        Counterparty counterparty = new Counterparty();
        ReflectionTestUtils.setField(counterparty, "id", 1L);
        counterparty.setName("Acme");
        counterparty.setLeiCode("LEI123");
        counterparty.setRegion("EMEA");

        Trade trade = new Trade();
        trade.setTradeRef("T-001");
        trade.setCounterparty(counterparty);

        when(tradeRepo.findByTradeRef("T-001")).thenReturn(Optional.of(trade));
        when(cpRepo.findById(1L)).thenReturn(Optional.of(counterparty));

        Counterparty result = service.counterpartyForTradeRef("T-001");

        assertThat(result).isSameAs(counterparty);
        verify(tradeRepo).findByTradeRef("T-001");
        verify(cpRepo).findById(1L);
    }

    @Test
    void throwsWhenCounterpartyCannotBeResolved() {
        TradeRepository tradeRepo = mock(TradeRepository.class);
        CounterpartyRepository cpRepo = mock(CounterpartyRepository.class);
        TradeLookupService service = new TradeLookupService(tradeRepo, cpRepo);

        Counterparty counterparty = new Counterparty();
        ReflectionTestUtils.setField(counterparty, "id", 1L);
        counterparty.setName("Acme");
        counterparty.setLeiCode("LEI123");
        counterparty.setRegion("EMEA");

        Trade trade = new Trade();
        trade.setTradeRef("T-002");
        trade.setCounterparty(counterparty);

        when(tradeRepo.findByTradeRef("T-002")).thenReturn(Optional.of(trade));
        when(cpRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.counterpartyForTradeRef("T-002"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No counterparty resolvable for trade T-002");
    }
}
