# Sprint 14 — Responsible AI assistant

## Goal

Replace the fixed three-question campus guide with a secure, useful assistant that can support university research, learning, programming and original image creation while remaining honest about evidence, access and academic integrity.

## Delivered

- A responsive Ask UniCircle workspace for desktop and mobile.
- Five explicit modes: Smart Help, University Research, Study Coach, Code Lab and Image Studio.
- Authenticated `/api/assistant/status` and `/api/assistant/ask` endpoints.
- A server-side OpenAI Responses API gateway; the browser never receives the provider key.
- Hosted web search for current questions, with returned sources shown as clickable links.
- Code Interpreter for Code Lab and image generation for Image Studio.
- Moderation before generation, input validation and a 20-request-per-10-minute account limit.
- A clear, working fallback when no provider key is configured.
- Guidance to verify important institutional information and use AI as learning support rather than misrepresenting assessed work.

## Engineering decisions

The assistant is a separate bounded module in the existing modular monolith. This avoids presenting human messaging as AI and keeps provider-specific code behind an `AssistantGateway`. Model names and the image model are environment-configurable so the integration can be updated without a rebuild.

The first pilot does not claim access to private university records. Public-web research and approved institutional integrations are treated as separate sources. A future university deployment would require a reviewed retrieval layer for each institution's approved documents, identity controls, retention rules, usage budgets and human escalation routes.

## Evaluation evidence

- Controller tests cover authentication, disabled-provider behaviour and validation limits.
- Frontend structure tests cover all five modes, source presentation and the integrity notice.
- Browser checks confirm responsive desktop and phone layouts and the no-key fallback journey.
- The full Maven test suite remains the regression gate before deployment.

## Remaining deployment dependency

Live AI requires a paid provider account and a private `OPENAI_API_KEY` in the hosting environment. The application deliberately remains in a visible setup-required state until that external dependency is supplied. Wider student testing should add budget alerts, monitoring, retention review and an institution-approved acceptable-use notice.
