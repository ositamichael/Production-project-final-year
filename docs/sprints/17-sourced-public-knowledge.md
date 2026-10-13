# Sprint 17 — Sourced public knowledge

## Aim

Answer a much wider range of stable factual questions without charging the student or pretending that a small set of fixed responses is a complete AI system.

## Implementation

- Reviewed local answers remain the first choice for supported computing, study, safety and UniCircle topics.
- An unfamiliar question offers an optional public-source lookup rather than returning the old generic topic list.
- The user must enable **Use public sources** for each browser session before an external request is made.
- The server screens for email addresses, long account-like numbers, student identifiers, URLs, credentials and other obvious confidential data.
- Only a shortened search topic is sent to the English Wikipedia Action API.
- A successful response includes a visible source link and a warning to verify important or changing facts.
- Current, personal medical, legal, financial and university-decision questions are redirected to an appropriate official source instead of being presented as verified advice.
- Missing, unsafe, unavailable and source-free results have different honest responses.

## Acceptance evidence

- “What is Python?” continues to use the fast reviewed local answer.
- “What is the best programming language?” explains that the choice depends on the task.
- “Who made Java?” can retrieve the Java article, select the creator sentence and show its source when public lookup is enabled.
- The same unfamiliar question does not leave the application when public lookup is disabled.
- A question containing an email address, long account-like number, student ID, URL or credential term is blocked before lookup.
- The application remains useful when Wikipedia is unavailable.

## Boundary

This feature is sourced retrieval, not a locally hosted general-purpose language model. It materially improves factual coverage, but it cannot answer every question perfectly, perform unrestricted reasoning, execute arbitrary code or replace official and professional advice.
