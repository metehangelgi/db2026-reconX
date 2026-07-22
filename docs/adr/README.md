# ReconX ADR Prompt Template

You are a senior enterprise software architect.

Write an Architecture Decision Record (ADR) using the Michael Nygard format.

Use exactly these sections:

# Title

## Status

Accepted | Date: <YYYY-MM-DD>

## Context

## Decision

## Consequences

---

System Context

ReconX is a near-production trade reconciliation platform built during the
Deutsche Bank TDI Graduate Programme.

Business Goal

ReconX compares internal trade records with external counterparty or custodian
feeds to identify reconciliation breaks, generate alerts, and maintain a full
audit trail for investigation.

Architecture

- React 19 + Vite frontend
- Spring Boot 3 REST API
- Java 25
- Spring Security JWT/RBAC
- PostgreSQL 16
- Liquibase migrations
- Apache Kafka
- Prometheus
- Grafana
- Docker Compose deployment

Scale

- ~50,000 trades per day
- ~18 million trades per year
- ~91 million trades across 5-year retention
- 10 concurrent reconciliation analysts
- Multiple Kafka topics
- Reconciliation and audit workloads

Decision To Record

<ONE SPECIFIC DECISION>

Alternatives Considered

<List at least 2-3 alternatives>

Constraints / Forces

<List at least 2-3 constraints>

Examples

- Partition trades by trade_date
- Store instrument metadata in JSONB
- Use GIN jsonb_path_ops index
- Use Kafka for trade ingestion
- Use JWT authentication
- Use Liquibase XML migrations
- Use materialized views for reconciliation reporting

Writing Rules

- Maximum 300 words
- Be specific to ReconX
- Include actual scale numbers
- Mention rejected alternatives
- Mention operational consequences
- Avoid generic architecture language
- Explain WHY the decision was made