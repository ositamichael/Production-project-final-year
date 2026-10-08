# ADR 0003: Federated identity with explicit university tenancy

- Status: Accepted
- Date: 2026-10-08

## Context

UniCircle must identify real members of participating universities without collecting their institutional passwords. It must also stop one institution's users or data being exposed to another by mistake.

## Decision

Use OpenID Connect for production sign-in. The external subject and issuer identify an account; the user's university membership is stored as an explicit tenant relationship. Protected database records carry a `university_id`, and server-side authorisation enforces the active tenant.

## Consequences

Real deployment requires identity-provider registration and approval from each university or an appropriate federation route. Email-domain checks alone are not sufficient proof of current student status. Cross-tenant access tests become mandatory before pilot deployment.
