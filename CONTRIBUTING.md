# Contributing to ScamShield

ScamShield is assessed as a Level 6 final-year project. Changes must therefore be reproducible, reviewable and honest about their evidence.

## Branch workflow

The historical prototype was developed directly on `main`. From the engineering-foundation sprint onward, work uses short-lived branches:

- `feat/<name>` for new capability;
- `fix/<name>` for a correction;
- `docs/<name>` for documentation-only work;
- `test/<name>` for test infrastructure.

Branches are merged through a pull request after automated checks pass. A tagged or otherwise recorded commit on `main` represents a demonstrable release. The project does not recreate historical branches or claim that earlier work used this process.

## Local setup

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements-dev.txt
```

## Required checks

```bash
.venv/bin/coverage run -m unittest discover -s tests -p "test_*.py" -v
.venv/bin/coverage report --fail-under=70
node tests/test_conversation.js
.venv/bin/python scripts/verify_evaluation.py
```

The branch-coverage threshold is a ratchet, not a quality target. Sprint 1 established a 60% gate against 65.5% measured coverage. Sprint 2 architecture tests raised measured coverage to 73.3%, so the gate is now 70% and must not be lowered to make a failing change pass.

## Definition of done

A change is complete only when:

1. Its acceptance criteria are met.
2. Relevant automated tests pass.
3. Important manual checks are recorded rather than merely claimed.
4. Privacy, model and data-provenance statements remain accurate.
5. Measured evaluation, synthetic demonstrations and public context remain visibly separate.
6. New limitations or failures are documented.
7. The pull request contains enough evidence for another person to reproduce the result.
