package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;

import java.util.List;

/**
 * TICKET-ADV043 / ADV045 — persistence boundary for reconciliation results.
 * ReconResult is a plain DTO/record (not a JPA @Entity); {@link ReconResultRepositoryImpl}
 * adapts it onto {@link ReconResultJpaRepository} / {@link com.dbtraining.reconx.repository.entity.ReconResultEntity}.
 */
public interface ReconResultRepository {
    ReconResult save(ReconResult result);
    List<ReconResult> findAll();
}
