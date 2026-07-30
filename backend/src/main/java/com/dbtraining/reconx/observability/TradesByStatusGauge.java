package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * TICKET-ADV092 — one polled Gauge per TradeStatus value, tagged "status",
 * feeding a Grafana pie chart showing the current breakdown of trades by
 * status. Unlike the ADV083 trade_created_total counter (which only fires at
 * create time), this reflects the *current* tally per status after
 * transitions via PATCH /v1/trades/{id}/status.
 */
@Component
public class TradesByStatusGauge {

    public TradesByStatusGauge(MeterRegistry registry, TradeRepository repo) {
        for (TradeStatus status : TradeStatus.values()) {
            Gauge.builder("trades_by_status", repo, r -> r.countByStatus(status))
                 .tag("status", status.name())
                 .description("Trades currently in a given status")
                 .register(registry);
        }
    }
}
