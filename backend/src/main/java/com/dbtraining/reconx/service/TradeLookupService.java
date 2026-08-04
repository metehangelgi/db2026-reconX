package com.dbtraining.reconx.service;
 
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/** TICKET-ADV082 — counterparty lookups are cached (cache name "counterparties", 1-minute TTL). */
@Service
public class TradeLookupService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;

    public TradeLookupService(TradeRepository tradeRepo, CounterpartyRepository cpRepo) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
    }

    // NOTE: caching lives on this top-level method rather than a private
    // "findCounterpartyById" helper — a @Cacheable on a same-class helper
    // called via `this.` would silently bypass Spring's proxy (the
    // classic self-invocation gotcha), so the whole lookup is cached here
    // by tradeRef instead.
    @Cacheable("counterparties")
    public Counterparty counterpartyForTradeRef(String tradeRef) {
        return tradeRepo.findByTradeRef(tradeRef)
                .map(t -> t.getCounterparty().getId())
                .flatMap(cpRepo::findById)
                .orElseThrow(() -> new NoSuchElementException(
                        "No counterparty resolvable for trade " + tradeRef));
    }
}
