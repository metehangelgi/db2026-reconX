package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.repository.entity.ReconResultEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TICKET-ADV045 — real, Spring-managed ReconResultRepository backed by
 * ReconResultJpaRepository, converting between the ReconResult DTO and
 * its JPA entity form.
 */
@Repository
public class ReconResultRepositoryImpl implements ReconResultRepository {

    private final ReconResultJpaRepository jpaRepository;

    public ReconResultRepositoryImpl(ReconResultJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReconResult save(ReconResult result) {
        ReconResultEntity saved = jpaRepository.save(new ReconResultEntity(
                result.tradeRef(),
                result.status().name(),
                result.discrepancyType(),
                result.details()));
        return toDto(saved);
    }

    @Override
    public List<ReconResult> findAll() {
        return jpaRepository.findAll().stream().map(this::toDto).toList();
    }

    private ReconResult toDto(ReconResultEntity entity) {
        return new ReconResult(
                entity.getTradeRef(),
                ReconResult.Status.valueOf(entity.getStatus()),
                entity.getDiscrepancyType(),
                entity.getDetails());
    }
}
