package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconBreakResponse;
import com.dbtraining.reconx.dto.ReconJobResponse;
import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.ReconJobRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import com.dbtraining.reconx.repository.entity.ReconJob;
import com.dbtraining.reconx.service.ReconJobRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET  /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT  /api/v1/recon/results/{id}/resolve
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;
    private final ReconJobRepository jobs;
    private final ReconJobRunner jobRunner;

    public ReconController(ReconBreakRepository breaks, ReconJobRepository jobs, ReconJobRunner jobRunner) {
        this.breaks = breaks;
        this.jobs = jobs;
        this.jobRunner = jobRunner;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    public ResponseEntity<Map<String, String>> runRecon(@Valid @RequestBody ReconRunRequest req) {
        String jobId = UUID.randomUUID().toString();

        ReconJob job = new ReconJob();
        job.setJobId(jobId);
        job.setFromDate(req.from());
        job.setToDate(req.to());
        job.setStatus("QUEUED");
        jobs.save(job);

        jobRunner.run(jobId, req.from(), req.to());

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/recon/jobs/" + jobId + "/results"))
                .body(Map.of("jobId", jobId, "status", "QUEUED"));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job-level stats for a recon run (total trades processed, breaks detected, status)")
    public ReconJobResponse job(@PathVariable String jobId) {
        return jobRunner.findJob(jobId);
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public List<ReconBreakResponse> results(@PathVariable String jobId) {
        return jobRunner.findResultsForJob(jobId);
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreakResponse> resolve(@PathVariable Long id,
                                              @Valid @RequestBody ResolutionRequest req) {
        ReconBreak rb = breaks.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Recon break not found: " + id));
        rb.resolve(req.note());
        breaks.save(rb);
        return ResponseEntity.ok(jobRunner.enrich(rb));
    }

    /** TICKET-ADV070 — resolution notes must be present and reasonably bounded. */
    public record ResolutionRequest(@NotBlank @Size(max = 500) String note) {}
}
