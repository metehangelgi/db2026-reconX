package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.observability.TradeStreamBroadcaster;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import com.dbtraining.reconx.dto.TradeEvent;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;

/**
 * ============================================================================
 * TICKET-ADV064 — TradeService.create (POST endpoint backing)
 * TICKET-ADV065 — update
 * TICKET-ADV066 — updateStatus (PATCH)
 * TICKET-ADV067 — softDelete
 * TICKET-ADV083 — increments trade_created_total Counter on create
 * TICKET-ADV129 — publishes TradeEvent on every state change
 * TICKET-ADV055/ADV056 — list() uses Specifications + filter query
 * TICKET-ADV104 — broadcasts to /v1/trades/stream SSE subscribers on create/update/updateStatus
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;
    private final TradeStreamBroadcaster broadcaster;

    public TradeService(TradeRepository tradeRepo,
                        CounterpartyRepository cpRepo,
                        InstrumentRepository instRepo,
                        TradeEventProducer events,
                        TradeMetrics metrics,
                        TradeStreamBroadcaster broadcaster) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
        this.broadcaster = broadcaster;
    }

    public Trade create(TradeRequest req, String actor) {
        tradeRepo.findByTradeRef(req.tradeRef()).ifPresent(t -> {
            throw new DuplicateTradeRefException(req.tradeRef());
        });

        var instrument = instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException("Instrument id=" + req.instrumentId()));
        var counterparty = cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException("Counterparty id=" + req.counterpartyId()));

        Trade trade = new Trade();
        trade.setTradeRef(req.tradeRef());
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());

        Trade saved = tradeRepo.save(trade);

        metrics.incrementTradeCreated();
        metrics.recordTradeValue(req.quantity().multiply(req.price()).doubleValue());
        events.publish(TradeEvent.created(saved.getTradeRef(), actor, null));
        broadcaster.broadcast(Map.of(
                "tradeRef", saved.getTradeRef(),
                "instrumentSymbol", saved.getInstrument().getSymbol(),
                "quantity", saved.getQuantity(),
                "price", saved.getPrice(),
                "status", saved.getStatus().name()
        ));

        return saved;
    }

    public Trade update(Long id, TradeRequest req, String actor) {
        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id=" + id));

        var instrument = instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException("Instrument id=" + req.instrumentId()));
        var counterparty = cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException("Counterparty id=" + req.counterpartyId()));

        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());

        Trade saved = tradeRepo.save(trade);
        events.publish(TradeEvent.updated(saved.getTradeRef(), actor, null, null));
        broadcaster.broadcast(Map.of(
                "tradeRef", saved.getTradeRef(),
                "instrumentSymbol", saved.getInstrument().getSymbol(),
                "quantity", saved.getQuantity(),
                "price", saved.getPrice(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }

    public Trade updateStatus(Long id, String status, String actor) {
        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id=" + id));

        TradeStatus newStatus;
        try {
            newStatus = TradeStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTradeException("Invalid status: " + status);
        }

        trade.setStatus(newStatus);
        Trade saved = tradeRepo.save(trade);
        events.publish(TradeEvent.updated(saved.getTradeRef(), actor, null, newStatus.name()));
        broadcaster.broadcast(Map.of(
                "tradeRef", saved.getTradeRef(),
                "instrumentSymbol", saved.getInstrument().getSymbol(),
                "quantity", saved.getQuantity(),
                "price", saved.getPrice(),
                "status", saved.getStatus().name()
        ));
        // instrument is already initialized above via getSymbol(); counterparty is
        // FetchType.LAZY and untouched so far. The controller maps this entity to a
        // TradeResponse (TradeMapper needs counterparty.name) after this transactional
        // method returns and the session closes — force-init while still open.
        Hibernate.initialize(saved.getCounterparty());
        return saved;
    }

    public void softDelete(Long id, String actor) {
        Trade t = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id=" + id));
        t.softDelete();
        tradeRepo.save(t);
        events.publish(TradeEvent.cancelled(t.getTradeRef(), actor, null));
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(LocalDate from, LocalDate to, String status, Long counterpartyId, String assetClass, Pageable pageable) {
        Specification<Trade> spec = tradeDateBetween(from, to)
                .and(hasStatus(status))
                .and(hasCounterparty(counterpartyId))
                .and(hasAssetClass(assetClass));
        Page<Trade> page = tradeRepo.findAll(spec, pageable);
        // instrument/counterparty are FetchType.LAZY; with open-in-view disabled the
        // session closes as soon as this @Transactional method returns, so the
        // controller (which maps Trade -> TradeResponse via TradeMapper) would hit a
        // LazyInitializationException. Force-init while the session is still open.
        page.forEach(t -> {
            Hibernate.initialize(t.getInstrument());
            Hibernate.initialize(t.getCounterparty());
        });
        return page;
    }
}
