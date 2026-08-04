package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TICKET-ADV137 — Event sourcing rebuild
 *
 * WHAT:    Reconstructs a trade's current state purely by folding its
 *          audit_log event stream, oldest first.
 * HOW:     TRADE_CREATED/TRADE_UPDATED set the running state to that event's
 *          after-snapshot; TRADE_CANCELLED clears it back to null. Note:
 *          this repo's AuditLogEntry stores before/after as raw JSON
 *          strings (not a parsed JsonNode) — see TradeEvent's own class
 *          doc for why — so this method mirrors that and returns
 *          Optional<String>, not Optional<JsonNode>.
 * WHY:     Proves the event log persisted by ADV132 is a genuine source of
 *          truth: any trade's state can be reconstructed from its events
 *          alone, the canonical event-sourcing pattern.
 * OBSERVE: rebuild(ref) after CREATED -> UPDATED -> CANCELLED returns
 *          Optional.empty(); without the CANCELLED it returns the last
 *          UPDATED event's after-snapshot.
 * ============================================================================
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;

    public TradeAggregator(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public Optional<String> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        String state = null;
        for (AuditLogEntry entry : events) {
            TradeEvent.EventType type = TradeEvent.EventType.valueOf(entry.getEventType());
            state = switch (type) {
                case TRADE_CREATED, TRADE_UPDATED -> entry.getAfterState();
                case TRADE_CANCELLED -> null;
            };
        }
        return Optional.ofNullable(state);
    }
}
