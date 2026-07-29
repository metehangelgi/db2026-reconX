package com.dbtraining.reconx.repository.entity;

/**
 * TICKET-ADV050 — Trade lifecycle status. Persisted as STRING (@Enumerated),
 * never ORDINAL, so reordering the enum never re-points existing rows.
 */
public enum TradeStatus {
    PENDING,
    CONFIRMED,
    SETTLED,
    CANCELLED
}
