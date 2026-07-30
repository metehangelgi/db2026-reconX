package com.dbtraining.reconx.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Job-level stats for the Reconciliation page KPI cards (total trades
 * processed, breaks detected, status) — not derivable from the breaks list
 * alone since clean/matched trades never produce a break row.
 */
public record ReconJobResponse(
        String jobId,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Integer tradesProcessed,
        Integer breaksDetected
) {}
