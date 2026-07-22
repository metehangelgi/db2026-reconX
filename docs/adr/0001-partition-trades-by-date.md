# ADR-0001 - Partition the trades table by trade_date

## Status

Accepted | Date: 2026-07-22

## Context

ReconX is a trade reconciliation platform that compares internal trade records
against external counterparty and custodian feeds.

The platform is expected to process approximately 50,000 trades per day and
retain trade history for five years. At steady state this represents more than
91 million trade records.

Most operational workflows, reconciliation jobs, dashboards, analyst searches,
and reporting queries are constrained by trade date ranges.

Alternatives considered:

- Single unpartitioned trades table
- Partition by counterparty
- Partition by reconciliation status

Constraints:

- Five-year retention policy
- High-volume daily ingestion
- Fast date-range reporting and reconciliation

## Decision

Use PostgreSQL RANGE partitioning on trade_date and create monthly partitions.

Partitions are named using the pattern:

trades_yYYYYmMM

A default partition captures unexpected dates and prevents failed inserts.

New partitions are pre-created through automated maintenance jobs.

## Consequences

### Positive

- Partition pruning improves query performance for date-bounded workloads.
- Archiving older data becomes a metadata operation rather than a large delete.
- Smaller indexes reduce maintenance overhead.
- Reconciliation reporting scales more effectively as data volume grows.

### Negative

- Partition management must be automated.
- Schema changes must consider all partitions.
- Composite keys may be required to satisfy PostgreSQL partitioning rules.
- Operational complexity increases compared to a single table.