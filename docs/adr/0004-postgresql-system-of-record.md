# ADR-0004 - PostgreSQL as the System of Record

## Status

Accepted | Date: 2026-07-22

## Context

ReconX receives a continuous stream of trade records from upstream systems and
must store reconciled and unreconciled trades for dashboard reporting,
investigation workflows, and audit purposes.

The platform is expected to process approximately 50,000 trades per day and
retain data for five years. This requires reliable persistence, strong
transactional guarantees, schema version control, and support for structured and
semi-structured trade data.

Alternatives considered:

- PostgreSQL
- H2 only
- NoSQL document database

Constraints:

- Five-year retention requirement
- Liquibase-managed schema migrations
- Support for JSON-based trade metadata
- Production-grade reliability requirements

## Decision

Use PostgreSQL 16 as the primary database for ReconX.

Use H2 only for local development, automated testing, and simulation
environments. PostgreSQL remains the authoritative system of record in UAT and
Production environments.

Liquibase will manage schema changes across all deployments.

## Consequences

### Positive

- Supports relational storage and JSONB query capabilities.
- Provides reliable persistence for reconciliation data.
- Integrates directly with Liquibase migration workflows.
- Well supported by Spring Boot and Spring Data JPA.
- Suitable for long-term retention and audit requirements.
- Enables collaborative development by allowing database schema changes to be version-controlled in Git through Liquibase changelogs.

### Negative

- Requires database administration and monitoring.
- Additional infrastructure compared with in-memory databases.
- H2 and PostgreSQL behavioural differences require compatibility testing.
- Backup and recovery processes must be maintained.