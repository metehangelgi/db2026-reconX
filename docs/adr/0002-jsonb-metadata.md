# ADR-0002 - Store instrument metadata using JSONB

## Status

Accepted | Date: 2026-07-22

## Context

ReconX supports multiple financial instrument types including equities,
fixed-income products, FX products, and future asset classes.

Each instrument may expose different attributes depending on source systems,
counterparties, and regulatory requirements.

Frequently altering the schema for new metadata fields would slow onboarding
and introduce unnecessary migration overhead.

Alternatives considered:

- Dedicated relational columns for every attribute
- Entity-Attribute-Value (EAV) model
- External document database

Constraints:

- Frequently changing metadata requirements
- PostgreSQL must remain the primary data store
- Flexible yet queryable storage model

## Decision

Store dynamic instrument attributes in a PostgreSQL JSONB column named
metadata.

Core business fields used for reconciliation and reporting remain as strongly
typed relational columns.

Metadata handling is delegated to JSONB for flexibility while maintaining
transactional consistency inside PostgreSQL.

## Consequences

### Positive

- New instrument attributes can be added without schema changes.
- Faster onboarding of new asset classes.
- Native PostgreSQL JSONB operators support querying and filtering.
- Single database technology for structured and semi-structured data.

### Negative

- Validation rules are less strict than traditional relational columns.
- Complex JSON queries are harder to maintain.
- Poor indexing choices may cause performance degradation.
- Developers must understand JSONB query patterns.