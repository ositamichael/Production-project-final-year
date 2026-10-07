# ScamShield Evaluation Plan

## Research question

How effectively can an explainable machine-learning system detect phishing messages that have been rewritten by generative AI, while providing explanations that non-technical users can understand?

## Planned comparison

1. Transparent TF-IDF-style linear baseline.
2. Stronger embedding or transformer-based classifier, subject to available resources.
3. ScamShield pipeline with explanation and safe-action guidance.

## Measures

- Precision, recall and F1 score.
- False-positive rate on legitimate messages.
- Robustness drop between original and AI-rewritten test messages.
- Explanation quality: relevance, factual support, clarity and completeness.
- Response time and resource cost.

## Experimental controls

- Freeze a test set before final model tuning.
- Record dataset provenance and label decisions.
- Use fixed random seeds and configuration files.
- Report confidence intervals or repeated-split variation where feasible.
- Include failure cases and negative findings.

## Sprint 4 final-evaluation gate

The earlier 12-message held-out set remains prototype evidence. It is not renamed or enlarged retrospectively. Final evaluation uses a new, separately versioned CSV and manifest governed by ADR 0005.

Before any prediction is opened, the gate must confirm:

- at least 50 legitimate and 50 phishing cases;
- opaque case identifiers, scenario, provenance and label rationale for every row;
- an adjudicated label review against a written policy;
- a matching SHA-256 digest and frozen row count;
- no exact or 90%-similar token overlap with training, challenge or earlier held-out messages;
- explicit confirmation that the final cases did not influence model rules or threshold selection.

The final run reports contextual and TF-IDF metrics on the same cases, confusion counts, failures and deterministic 95% case-bootstrap intervals for the contextual method. Those intervals describe uncertainty within the frozen sample; they do not prove representativeness or real-world reliability.

If the required CSV is absent, the correct state is **evaluation pending**. Test fixtures, synthetic demonstrations and TrustLab transformations must not populate the final dashboard result.

## Definition of success

The product provides a complete user workflow, produces repeatable results, gives explanations tied to the input text, and supports a defensible conclusion about the trade-off between detection performance and user-understandability.
