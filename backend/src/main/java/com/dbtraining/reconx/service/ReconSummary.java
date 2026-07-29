package com.dbtraining.reconx.service;

/**
 * TICKET-ADV038 — Aggregate counts produced by {@link ReconSummaryCollector}.
 */
public record ReconSummary(long total, long matched, long broken) {
    public static ReconSummary empty() { return new ReconSummary(0, 0, 0); }

    public static final class Builder {
        long total;
        long matched;
        long broken;
    }
}
