# Debugging log

This log records investigated problems, evidence and outcomes. It begins with the formal engineering workflow on 7 October 2026 and does not claim to reconstruct every earlier debugging session.

## Entry template

- **Date and branch:**
- **Observed behaviour:**
- **Expected behaviour:**
- **Reproduction:**
- **Evidence inspected:**
- **Debugger or diagnostic technique:**
- **Root cause:**
- **Correction:**
- **Regression test:**
- **Remaining risk:**

## 2026-10-07 — clean-machine test startup failure

- **Branch:** `feat/sprint-1-engineering-foundation`
- **Observed behaviour:** Python tests failed during import with `ModuleNotFoundError` for NumPy and certifi; the JavaScript conversation tests passed.
- **Expected behaviour:** A contributor should be able to create an isolated environment and run every check from documented commands.
- **Reproduction:** Run the Python test suite with the system interpreter before installing project dependencies.
- **Evidence inspected:** Python traceback, installed-package state, `requirements.txt` and existing README setup instructions.
- **Diagnostic technique:** Import traceback inspection and environment comparison. A repeatable debugger configuration was also added for server, tests and evaluation work; first breakpoint evidence will be recorded when investigating application logic rather than falsely claimed here.
- **Root cause:** The system interpreter was not an isolated project environment and did not contain runtime dependencies.
- **Correction:** Added `requirements-dev.txt`, isolated-environment instructions and CI installation steps.
- **Regression control:** CI now installs dependencies on a clean runner before executing tests and coverage.
- **Remaining risk:** Local execution still depends on the contributor following the environment setup instructions.

## 2026-10-07 — standalone evaluation verifier import failure

- **Branch:** `feat/sprint-1-engineering-foundation`
- **Observed behaviour:** The test and coverage suite passed, but `python scripts/verify_evaluation.py` failed with `ModuleNotFoundError: src`.
- **Expected behaviour:** The documented verifier command should work from the repository root without requiring a manually configured `PYTHONPATH`.
- **Reproduction:** Install development dependencies and run the verifier as a file from the repository root.
- **Evidence inspected:** Traceback, Python's script import path and the verifier's import order.
- **Diagnostic technique:** Traceback-led path inspection; the failure was isolated after the successful test and coverage stages.
- **Root cause:** Running a file from `scripts/` places that directory—not the repository root—at the beginning of `sys.path`.
- **Correction:** Resolve the repository root from `__file__` and add it to the import path before importing `src.evaluate`.
- **Regression control:** The exact standalone command runs in GitHub Actions after the tests.
- **Remaining risk:** A future packaging step should replace this bootstrap with an installable project package.
