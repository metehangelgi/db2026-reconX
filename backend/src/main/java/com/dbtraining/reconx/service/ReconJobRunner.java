package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconBreakResponse;
import com.dbtraining.reconx.dto.ReconJobResponse;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.ReconJobRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.TradeSpecifications;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import com.dbtraining.reconx.repository.entity.ReconJob;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import io.micrometer.core.annotation.Timed;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Does the actual work behind POST /v1/recon/run.
 *
 * WHAT:    This schema has no separate external-counterparty-feed table —
 *          `trades` is the single source of truth, so there is nothing to
 *          diff internal-vs-external the way ReconciliationEngine (the
 *          sealed-TradeType-based Day 3 engine) does. "Reconciling" here
 *          means flagging, within the requested date range, trades whose
 *          own lifecycle status indicates a real problem: CANCELLED (never
 *          settled) or PENDING (never confirmed). CONFIRMED/SETTLED trades
 *          are treated as clean matches and produce no break row. This is a
 *          deliberate scope decision, not a stand-in for the full two-feed
 *          design the original ticket describes — that design needs a
 *          second data source this project doesn't have.
 * HOW:     Runs on a background thread (@Async — already enabled via
 *          @EnableAsync on ReconxApplication) so POST /recon/run keeps
 *          returning 202 immediately. Drives the ReconJob row through its
 *          existing QUEUED -> RUNNING -> COMPLETE lifecycle (columns were
 *          already in the schema, just never populated) and writes one
 *          ReconBreak row per flagged trade, tagged with this run's jobId.
 * ============================================================================
 */
@Service
public class ReconJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ReconJobRunner.class);

    private final TradeRepository tradeRepo;
    private final ReconJobRepository jobRepo;
    private final ReconBreakRepository breakRepo;

    public ReconJobRunner(TradeRepository tradeRepo, ReconJobRepository jobRepo, ReconBreakRepository breakRepo) {
        this.tradeRepo = tradeRepo;
        this.jobRepo = jobRepo;
        this.breakRepo = breakRepo;
    }

    @Async
    @Transactional
    @Timed(value = "reconciliation.duration", description = "Wall time of a recon job run",
           percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public void run(String jobId, LocalDate from, LocalDate to) {
        ReconJob job = jobRepo.findByJobId(jobId).orElse(null);
        if (job == null) {
            log.warn("recon job {} vanished before it could run", jobId);
            return;
        }

        job.setStatus("RUNNING");
        job.setStartedAt(Instant.now());
        jobRepo.save(job);

        List<Trade> trades = tradeRepo.findAll(TradeSpecifications.tradeDateBetween(from, to));

        int breaksDetected = 0;
        for (Trade trade : trades) {
            String discrepancyType = classify(trade);
            if (discrepancyType == null) continue;

            ReconBreak brk = new ReconBreak();
            brk.setTradeId(trade.getId());
            brk.setJobId(jobId);
            brk.setDiscrepancyType(discrepancyType);
            brk.setDetectedAt(Instant.now());
            brk.setPriority(computePriority(trade, discrepancyType));
            breakRepo.save(brk);
            breaksDetected++;
        }

        job.setStatus("COMPLETE");
        job.setFinishedAt(Instant.now());
        job.setTradesProcessed(trades.size());
        job.setBreaksDetected(breaksDetected);
        jobRepo.save(job);

        log.info("recon job {} complete: {} trades processed, {} breaks detected", jobId, trades.size(), breaksDetected);
    }

    /** @return a discrepancy type if this trade should be flagged as a break, or {@code null} if it's a clean match. */
    private String classify(Trade trade) {
        if (trade.getStatus() == TradeStatus.CANCELLED) return "TRADE_CANCELLED";
        if (trade.getStatus() == TradeStatus.PENDING)   return "UNCONFIRMED_PENDING";
        return null;
    }

    /**
     * Real, computed priority — not a placeholder value:
     * HIGH:   the trade already cancelled without settling — the highest-risk case.
     * MEDIUM: still pending 3+ days after its trade date — stale enough to need attention.
     * LOW:    everything else (recently-booked pending trades within normal settlement cycle).
     */
    private String computePriority(Trade trade, String discrepancyType) {
        if ("TRADE_CANCELLED".equals(discrepancyType)) return "HIGH";
        long daysSinceTrade = ChronoUnit.DAYS.between(trade.getTradeDate(), LocalDate.now());
        return daysSinceTrade > 3 ? "MEDIUM" : "LOW";
    }

    /**
     * Enriches the raw ReconBreak rows for a job with the trade/instrument/
     * counterparty fields the entity itself doesn't carry (it only stores a
     * bare tradeId), so the Reconciliation page can display and filter by
     * counterparty without one lookup per row.
     */
    @Transactional(readOnly = true)
    public List<ReconBreakResponse> findResultsForJob(String jobId) {
        List<ReconBreak> breaks = breakRepo.findByJobId(jobId);
        List<Long> tradeIds = breaks.stream().map(ReconBreak::getTradeId).distinct().toList();

        Map<Long, Trade> tradesById = tradeRepo.findAllById(tradeIds).stream()
                .peek(t -> { Hibernate.initialize(t.getInstrument()); Hibernate.initialize(t.getCounterparty()); })
                .collect(Collectors.toMap(Trade::getId, Function.identity()));

        return breaks.stream().map(b -> toResponse(b, tradesById.get(b.getTradeId()))).toList();
    }

    /** Same enrichment as {@link #findResultsForJob}, for a single break (used after resolving one). */
    @Transactional(readOnly = true)
    public ReconBreakResponse enrich(ReconBreak b) {
        Trade t = tradeRepo.findById(b.getTradeId()).orElse(null);
        if (t != null) {
            Hibernate.initialize(t.getInstrument());
            Hibernate.initialize(t.getCounterparty());
        }
        return toResponse(b, t);
    }

    private ReconBreakResponse toResponse(ReconBreak b, Trade t) {
        return new ReconBreakResponse(
                b.getId(),
                b.getTradeId(),
                t != null ? t.getTradeRef() : null,
                t != null ? t.getInstrument().getSymbol() : null,
                t != null ? t.getCounterparty().getId() : null,
                t != null ? t.getCounterparty().getName() : null,
                t != null ? t.getQuantity() : null,
                b.getJobId(),
                b.getDiscrepancyType(),
                b.getStatus(),
                b.getPriority(),
                b.getDetectedAt(),
                b.getResolvedAt(),
                b.getResolutionNote());
    }

    /** Job-level stats for the Reconciliation page KPI cards. */
    @Transactional(readOnly = true)
    public ReconJobResponse findJob(String jobId) {
        ReconJob job = jobRepo.findByJobId(jobId)
                .orElseThrow(() -> new TradeNotFoundException("Recon job not found: " + jobId));
        return new ReconJobResponse(
                job.getJobId(),
                job.getFromDate(),
                job.getToDate(),
                job.getStatus(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getTradesProcessed(),
                job.getBreaksDetected());
    }
}
