# ADR 0001: Use Java 21 and Spring Boot

- Status: Accepted
- Date: 2026-10-08

## Context

The project needs secure web APIs, university single sign-on, relational data, real-time messaging, test support and a deployment model that can be discussed with university IT teams. The student is also expected to demonstrate the software engineering methods taught on the course.

## Decision

Use Java 21 as the target language level and Spring Boot 4.1.1 as the application framework. Build with Maven Wrapper so the project uses a pinned build tool without requiring a system Maven installation.

## Consequences

Spring Security, WebSocket, validation, JPA, health monitoring and established testing libraries are available in one ecosystem. Java's type system supports clear interfaces and domain models. The cost is a steeper learning curve and more configuration than a simple scripting-language prototype, so the work will be delivered incrementally with tests and architecture records.
