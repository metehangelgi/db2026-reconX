# ADR-0005 - Use Apache Kafka for Trade Data Ingestion

## Status

Accepted | Date: 2026-07-21

## Context

ReconX continuously receives trade records from internal trade systems and
external counterparties.

The platform must support asynchronous processing between trade producers and
backend services while remaining resilient during spikes in trade volume.

At approximately 50,000 trades per day, directly coupling producers to database
operations would reduce scalability and introduce operational risks.

Alternatives considered:

- Direct database writes
- REST-based service chaining
- Scheduled database polling

Constraints:

- High-volume trade ingestion
- Future scalability requirements
- Reliable message delivery
- Loose coupling between services

## Decision

Use Apache Kafka as the message broker between trade producers and backend
processing services.

Trade events are published to Kafka topics and consumed by dedicated services
responsible for reconciliation, auditing, alerting, and persistence.

Dead-letter queues (DLQ) are used to handle failed message processing.

## Consequences

### Positive

- Decouples trade generation from persistence.
- Supports asynchronous processing and scaling.
- Provides durable messaging and replay capabilities.
- Improves system resilience during traffic spikes.
- Enables future event-driven integrations.

### Negative

- Adds infrastructure and operational complexity.
- Requires monitoring of brokers, topics, and consumers.
- Message ordering considerations must be addressed.
- Troubleshooting distributed event flows is more complex than synchronous
  systems.