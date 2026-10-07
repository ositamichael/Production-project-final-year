# ScamShield architecture

## Current system

```mermaid
flowchart LR
    U[User] --> UI[Browser interface]
    UI -->|redacted text| API[Python HTTP server]
    UI -->|local preview and OCR| OCR[Browser OCR]
    API --> DET[TF-IDF and contextual detector]
    DET --> EXP[Evidence-linked explanation]
    API --> DATA[Versioned CSV and JSON]
    DATA --> DASH[Research dashboard]
```

The current implementation is intentionally small. Browser code handles the interface and local media workflow. The Python server exposes analysis, dashboard and constrained public-URL metadata endpoints. Versioned files contain training, evaluation, synthetic and public-context data.

## Target architecture for Sprints 2–3

```mermaid
flowchart TB
    UI[Scanner / Evidence Lab / TrustLab / Dashboard]
    UI --> EA[Evidence adapters]
    EA --> TXT[Text adapter]
    EA --> IMG[OCR text adapter]
    EA --> VID[Recording-frame text adapter]
    EA --> URL[URL-pattern adapter]
    EA --> APP[Analysis service]
    APP --> DS[Detector strategy]
    DS --> TF[TF-IDF baseline]
    DS --> CTX[Contextual detector]
    DS --> FUT[Future comparison model]
    APP --> REPO[Case repository interface]
    REPO --> SQL[(SQLite research store)]
    APP --> EX[Evidence-linked explanation]
    SQL --> EVAL[Versioned evaluation and replay]
    EVAL --> DASH
```

### Intended patterns

- **Strategy:** each detector implements the same analysis contract, allowing controlled comparison without conditional logic throughout the application.
- **Adapter:** text, OCR output, recording-frame output and URL-pattern evidence are converted into one validated evidence representation.
- **Repository:** application services depend on a storage interface rather than SQLite details, enabling isolated tests and future storage changes.

These patterns are target decisions, not a claim about the present code. Their implementation and tests are Sprint 2 acceptance criteria.

## Current trust boundaries

- Uploaded images and recordings are previewed in the browser.
- OCR output must be reviewed before text is analysed.
- The server receives analysed text, not raw uploaded media, in the current design.
- Public URL inspection accepts only public HTTP(S) destinations and returns technical metadata, not a reputation verdict.
- Measured evaluation, synthetic demonstrations and public context use separate data sources and labels.
