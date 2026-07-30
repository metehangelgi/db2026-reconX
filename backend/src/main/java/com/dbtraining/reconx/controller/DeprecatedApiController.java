package com.dbtraining.reconx.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * TICKET-ADV080 — API versioning + deprecation example
 *
 * WHAT:    Demonstrates how a retired API version is cleanly sunset: instead
 *          of a bare 404 (which tells a caller nothing), a retired endpoint
 *          returns 410 Gone with Deprecation/Sunset/Link headers so clients
 *          can programmatically detect the retirement and find the successor.
 * WHY:     Pinning /v1 as the live contract now is what lets a future /v2 ship
 *          without breaking in-flight clients — old versions keep responding
 *          (with this exact shape) until their sunset date passes.
 * ============================================================================
 */
@RestController
@RequestMapping("/v0/trades")
@Tag(name = "deprecated", description = "Retired API surface — example of the deprecation contract")
public class DeprecatedApiController {

    @GetMapping
    @Deprecated(since = "v1.4.0", forRemoval = true)
    @Operation(summary = "Retired — use /v1/trades instead", deprecated = true)
    public ResponseEntity<Void> deprecatedList() {
        return ResponseEntity.status(HttpStatus.GONE)
                .header("Deprecation", "true")
                .header("Sunset", "Wed, 31 Dec 2026 23:59:59 GMT")
                .header(HttpHeaders.LINK, "</api/v1/trades>; rel=\"successor-version\"")
                .build();
    }
}
