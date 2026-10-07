# TrustLab failure analysis — Sprint 3

**Replay version:** `trustlab-v1`  
**Model version:** `contextual-baseline-v5`  
**Development seed:** `challenge_messages-v1`  
**Role:** Development robustness analysis; not independent final evaluation

## Question

Does the detector preserve correct decisions when a controlled message is padded, made more polite, stripped of obvious urgency or reformatted, and does it lower its warning when scam wording is clearly quoted or refused?

## Result summary

The contextual detector produced 16 failures across 144 transformed messages. All were false alarms; there were no missed scam requests in this controlled replay.

| Transformation | Contextual failures | Interpretation |
|---|---:|---|
| Identity | 0 | The original 24-case development suite still reproduces. |
| Neutral padding | 0 | Short neutral openings and closings did not change correctness. |
| Polite tone | 0 | Softer wording did not hide the controlled scam requests. |
| Urgency softening | 0 | Removing explicit deadlines did not create a missed scam in this suite. |
| Formatting variation | 0 | Replacing selected punctuation did not change correctness. |
| Awareness quotation | 4 | Four multi-clause scam quotations still exposed an actionable clause to the detector. |
| Denied-request context | 12 | Every quoted scam followed by an explicit refusal remained flagged. |

The TF-IDF baseline produced 47 failures: 40 false alarms and seven missed scams. Its failures occurred in every transformation family, including five failures on the unmodified identity cases. This supports the value of contextual rules on this development suite but is not a general performance claim.

## Root-cause interpretation

The contextual detector removes clauses that contain recognised educational or protective language. It does not model quotation scope across several sentences. When a scam request is copied inside quotation marks and followed by “I did not act” or “do not follow,” the quoted clauses can still be analysed as direct instructions. This creates an observable false-alarm path.

The issue is not corrected in this sprint for three reasons:

1. The failure was discovered on development data, so immediately changing the detector would turn the same transformed cases into tuning targets.
2. Reliable quotation handling requires explicit scope rules and new development examples, not a phrase-specific exception.
3. Any detector change must receive a new model version and be assessed later on untouched independent data.

## Decision and next action

- Keep the failures in `data/trustlab_results.json`.
- Do not alter `contextual-baseline-v5` during this replay.
- Treat quotation/refusal scope as a candidate improvement for a future versioned detector.
- Design any new quotation parser against a separate development set, then freeze it before Sprint 4 evaluation.
- Continue to explain that the scanner provides decision support and can raise false alarms when security-awareness material contains realistic scam wording.

## Reproduction

```bash
.venv/bin/python -m src.trustlab
.venv/bin/python scripts/verify_trustlab.py
```

The replay verifier compares the complete versioned export, including the transformation manifest, dataset digest, case digests, outcomes, metrics and failure list.
