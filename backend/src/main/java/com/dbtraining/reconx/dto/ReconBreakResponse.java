package com.dbtraining.reconx.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Enriched view of a {@code ReconBreak} for the Reconciliation page —
 * joins in the trade/instrument/counterparty fields the raw entity doesn't
 * carry (it only stores a bare {@code tradeId}), so the frontend can
 * display and filter by counterparty without a second round-trip per row.
 */
public record ReconBreakResponse(
        Long id,
        Long tradeId,
        String tradeRef,
        String instrumentSymbol,
        Long counterpartyId,
        String counterpartyName,
        BigDecimal quantity,
        String jobId,
        String discrepancyType,
        String status,
        String priority,
        Instant detectedAt,
        Instant resolvedAt,
        String resolutionNote
) {}
