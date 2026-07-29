package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TICKET-ADV043 / ADV045 — wraps ReconciliationEngine and persists every
 * result via ReconResultRepository, one save() call per result.
 */
@Service
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repo;

    public ReconciliationService(ReconciliationEngine engine, ReconResultRepository repo) {
        this.engine = engine;
        this.repo = repo;
    }

    public List<ReconResult> runRecon(List<TradeType> internal, List<TradeType> external, ReconciliationRule rule) {
        List<ReconResult> results = engine.reconcile(internal, external, rule);
        results.forEach(repo::save);
        return results;
    }
}
