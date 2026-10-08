# Contributing to UniCircle

## Development workflow

1. Create one feature branch per sprint or independently reviewable change.
2. Link work to an issue, requirement or sprint objective.
3. Add or update tests with every behavioural change.
4. Run `./mvnw clean verify` before opening a pull request.
5. Record material architecture choices in `docs/adr`.
6. Use a pull request and review the complete diff before merging to `main`.

## Commit style

Use short, action-led messages, for example:

- `feat: add university tenant boundary`
- `test: verify connection consent rules`
- `docs: record OIDC identity decision`
- `fix: prevent cross-university profile lookup`

## Definition of done

A feature is not complete until its acceptance criteria, error states, accessibility, security implications, tests and documentation have been considered. Prototype behaviour must be labelled honestly and must not be described as a live integration.
