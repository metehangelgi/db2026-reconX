# ADR-0006 - Database-Backed Authentication Strategy

## Status

Accepted | Date: 2026-07-21

## Context

ReconX provides dashboard access and REST APIs for multiple user types,
including:

- ADMIN
- TRADER
- VIEWER
- RECON_ANALYST

User accounts, roles, and permissions must be managed securely without
requiring application code changes or redeployments.

Alternatives considered:

- Hard-coded credentials
- In-memory user accounts
- Database-backed authentication

Constraints:

- Role-based access control requirements
- Security and auditability requirements
- Future enterprise identity integration
- Spring Security architecture

## Decision

Use a dedicated authentication database integrated with Spring Security instead
of hard-coded credentials.

User accounts, roles, and permissions are stored persistently and managed
through database records.

Application authorization decisions are based on assigned roles and privileges.

## Consequences

### Positive

- User accounts can be managed without code changes.
- Supports role-based access control (RBAC).
- Improves auditability and security posture.
- Supports future integration with corporate identity providers.
- Scales more effectively than hard-coded user management.

### Negative

- Additional implementation effort is required.
- Authentication database requires maintenance and monitoring.
- Credential storage and access controls must be secured.
- Authentication services become critical platform dependencies.

### Rejected Alternative

Hard-coded credentials were rejected because credential changes would require
application updates and redeployment. The approach does not provide suitable
user management, auditability, or security for UAT and Production
environments.