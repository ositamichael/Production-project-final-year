# ADR 0002: Begin with a modular monolith

- Status: Accepted
- Date: 2026-10-08

## Context

UniCircle covers several campus capabilities. Splitting every capability into a separate deployed service would add network, deployment, observability and data-consistency complexity before the product rules have been validated.

## Decision

Build one deployable Spring Boot application with explicit Spring Modulith boundaries. Capabilities communicate through published interfaces and domain events where useful.

## Consequences

The first deployment remains realistic for a final-year project, local development is simpler, and transactions can protect booking and messaging data. Module verification prevents accidental coupling. A module can be extracted later when measured scale or organisational ownership justifies it.
