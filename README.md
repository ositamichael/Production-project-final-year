# ScamShield

ScamShield is a final-year computer science project exploring whether an explainable text-classification system can help non-technical users recognise phishing and scam messages, including messages rewritten to sound more convincing.

For a clean first explanation of the project, see [`docs/project_overview.md`](docs/project_overview.md) and the assessor-facing [Project Overview document](outputs/ScamShield_Project_Overview_Updated.docx).

The formal software-engineering workflow begins with Sprint 1 on 7 October 2026. See the [software engineering plan](docs/software_engineering_plan.md), [requirements traceability matrix](docs/requirements_traceability.md), [architecture record](docs/architecture.md), [Sprint 1 record](docs/sprints/01-engineering-foundation.md), [Sprint 2 record](docs/sprints/02-architecture-data.md), [Sprint 3 record](docs/sprints/03-trustlab.md) and [Sprint 4 record](docs/sprints/04-independent-evaluation.md). These records intentionally distinguish the earlier iterative prototype from the branch-and-pull-request process used from this sprint onward.

## Project boundary

This is a controlled academic prototype. It analyses pasted fictional or public message text and user-approved OCR text. It does not connect to email accounts, send messages, make financial decisions or replace human judgement. Its optional public-web checker requests technical response metadata for a supplied public URL; that check is not a safety or reputation verdict.

## Current prototype

The current vertical slice uses a transparent TF-IDF-style classifier implemented with Python and NumPy, together with a contextual rule layer. The rules look for phrases that combine an action with a credential, payment, link, urgency or trusted-service reference. Negated advice such as “never share your password” and quoted scam examples are treated as awareness content rather than requests. Explanations quote the observed phrase and state what it means; the score is a screening signal, not calibrated probability.

The interface also includes a research dashboard and a multimodal evidence lab. The evidence lab accepts redacted chat screenshots and short screen recordings. Screenshots are read with browser-side OCR; recordings are previewed locally, sampled at four to seven frames and passed through the same browser-side OCR before duplicate lines are combined. The user must review or correct the resulting text before sending that text—not the media file—for analysis. A demonstration library contains 20 random screenshot examples and 12 short recording examples, balanced across synthetic scam and legitimate cases; recording demonstrations use the real sampled-frame OCR path rather than inserting pre-supplied text. The lab also records transparent human annotations for visible warning signs. Its evidence cards link to public UK sources, while its charts are explicitly labelled synthetic demonstration data or official context. This prevents the project from presenting fictional test scenarios as claims about real banks, universities, football clubs or other named organisations. See `docs/data_provenance.md` and `docs/multimodal_evidence_method.md`.

The current interface is published at https://scamshield-dahk.onrender.com/.
Deployed verification endpoints are `/health` and `/api/meta`; they expose the non-secret build identifier and model version so a release can be checked without exposing credentials.

### Architecture and optional research metadata

The analysis path uses Evidence Adapters, interchangeable Detector Strategies and an Analysis Repository contract. The deployed default uses a stateless repository, so message text is not retained. For controlled local research only, `SCAMSHIELD_RESEARCH_DB=/path/to/research.sqlite3` enables SQLite metadata recording. That store contains a one-way text digest, length, evidence kind, detector version and result fields—not the raw message or media. See [`docs/data_model.md`](docs/data_model.md).

### Public-web verification

The Scanner separates two URL checks. **Check patterns** reviews the written address locally. **Check online** asks the server for public DNS, HTTPS, redirect, HTTP-status and content-type metadata. Private/internal destinations and non-standard web ports are blocked, and page bodies are not analysed. This is not malware analysis, domain reputation or proof that a site is safe; official NCSC, FCA and Report Fraud links are provided for independent follow-up.

## Run locally

```bash
python3 src/server.py
```

Open `http://127.0.0.1:8000` in a browser.

## Publish with GitHub and Render

1. Use the university project repository: [`ositamichael/Production-project-final-year`](https://github.com/ositamichael/Production-project-final-year).
2. In Render, choose **New Web Service** and connect that repository.
3. Render will use `render.yaml`, install `requirements.txt` and start the Python server.
4. Set the service's port handling if required by the hosting provider before publishing.

The public version should use fictional/public evidence only. Do not enable permanent storage of uploaded private images without a privacy notice, consent and deletion controls.

## Research evidence to collect

- Dataset provenance and cleaning decisions
- Fixed train/test split
- Baseline and improved-model metrics
- Original versus AI-rewritten robustness results
- Explanation-quality rubric
- Usability or expert-review evidence
- Threat model, ethics record and risk updates

See `docs/evaluation_plan.md`, `docs/ethics_and_scope.md` and `docs/model_card.md`.

## Reproduce the held-out evaluation

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python -m src.evaluate
.venv/bin/python -m unittest -v tests.test_core tests.test_dashboard tests.test_evaluation
```

For the complete development checks, install `requirements-dev.txt` and follow [`CONTRIBUTING.md`](CONTRIBUTING.md). `scripts/verify_evaluation.py` recalculates both evaluation sets and checks the versioned metrics and case decisions without treating the generated run date as a model result.

## Reproduce the TrustLab development replay

```bash
.venv/bin/python -m src.trustlab
.venv/bin/python scripts/verify_trustlab.py
```

TrustLab deterministically applies neutral padding, polite tone, urgency softening, formatting variation, explicit awareness quotation and denied-request context to the 24-message development challenge set. It compares the contextual detector with the TF-IDF baseline, records prediction stability and correctness separately, and preserves every failure for review. It never transforms the final held-out set. The checked-in `data/trustlab_results.json` stores case identifiers, scores, labels and SHA-256 digests rather than duplicated message text; optional SQLite replay storage follows the same boundary.

The checked-in result is `data/evaluation_results.json`. It contains a small, hand-curated held-out set and must not be presented as real-world accuracy. After correcting the unexplained false-alarm path, the current reproducible run reports precision 1.000, recall 1.000 and F1 1.000 on 12 messages, with 0 false alarms and 0 missed scams. This perfect result is not evidence of real-world reliability; the dashboard explains the small sample and validation limitations.

The current rules build is `contextual-baseline-v5`, using a 36/100 screening-score threshold. The regression suite covers clause-level negation, mixed protective and malicious instructions, helpdesk authentication-code requests, changed-number family impersonation, discouraged verification, protective security advice, complete monetary amounts and exact supporting excerpts. A separate 24-message development challenge set is retained for regression checks. Because known failures have now been corrected against it, it is not described as an independent final test set.

## Final independent evaluation status

The final evaluation is **pending**, not missing by accident. Sprint 4 adds a guarded runner that requires a frozen dataset, matching SHA-256 manifest, written label policy, completed label review, at least 50 cases in each class and no exact or near-duplicate overlap with development data. CI reports the pending state without manufacturing charts or metrics; once a legitimate frozen dataset is present, the same check reproduces the final export and deterministic 95% bootstrap intervals.

```bash
.venv/bin/python scripts/verify_final_evaluation.py
```

Templates are provided in `data/independent_evaluation_template.csv` and `data/independent_evaluation_manifest.template.json`. They are process scaffolding, not measured evidence.

## GitHub Stats

![Michael's GitHub Stats](https://github-readme-stats.vercel.app/api?username=ositamichael&theme=radical&show_icons=true)
