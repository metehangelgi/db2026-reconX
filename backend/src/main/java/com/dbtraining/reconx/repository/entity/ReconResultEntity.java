package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;

/**
 * TICKET-ADV045 — JPA persistence record for a reconciliation result,
 * backing {@link com.dbtraining.reconx.repository.ReconResultRepository}.
 */
@Entity
@Table(name = "recon_results")
public class ReconResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "discrepancy_type", length = 30)
    private String discrepancyType;

    @Column(columnDefinition = "TEXT")
    private String details;

    public ReconResultEntity() {}

    public ReconResultEntity(String tradeRef, String status, String discrepancyType, String details) {
        this.tradeRef = tradeRef;
        this.status = status;
        this.discrepancyType = discrepancyType;
        this.details = details;
    }

    public Long getId()                { return id; }
    public String getTradeRef()        { return tradeRef; }
    public String getStatus()          { return status; }
    public String getDiscrepancyType() { return discrepancyType; }
    public String getDetails()         { return details; }
}
