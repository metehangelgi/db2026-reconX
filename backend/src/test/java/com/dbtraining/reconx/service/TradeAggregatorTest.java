package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeAggregatorTest {

    private final AuditLogRepository repo = mock(AuditLogRepository.class);
    private final TradeAggregator aggregator = new TradeAggregator(repo);

    @Test
    void rebuild_noEvents_returnsEmpty() {
        when(repo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of());

        assertThat(aggregator.rebuild("TRD-001")).isEmpty();
    }

    @Test
    void rebuild_createdThenUpdated_returnsLastAfterSnapshot() {
        when(repo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of(
                entry("TRADE_CREATED", null, "{\"status\":\"PENDING\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"PENDING\"}", "{\"status\":\"CONFIRMED\"}")
        ));

        assertThat(aggregator.rebuild("TRD-001")).contains("{\"status\":\"CONFIRMED\"}");
    }

    @Test
    void rebuild_createdUpdatedThenCancelled_returnsEmpty() {
        when(repo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of(
                entry("TRADE_CREATED", null, "{\"status\":\"PENDING\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"PENDING\"}", "{\"status\":\"CONFIRMED\"}"),
                entry("TRADE_CANCELLED", "{\"status\":\"CONFIRMED\"}", null)
        ));

        assertThat(aggregator.rebuild("TRD-001")).isEqualTo(Optional.empty());
    }

    private AuditLogEntry entry(String eventType, String before, String after) {
        return new AuditLogEntry(
                java.util.UUID.randomUUID().toString(), "TRD-001", eventType, Instant.now(), "tester", before, after);
    }
}
