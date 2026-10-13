# Sprint 16 — No-cost basic assistant

## Aim

Keep Ask UniCircle genuinely useful on the staging deployment without requiring a paid model account, while making a clear distinction between supported local guidance and future advanced AI capabilities.

## Design

The server now selects one of two implementations behind the existing assistant gateway:

- **Basic** uses a deterministic Java knowledge engine stored and reviewed with the application.
- **Advanced** uses the existing external provider integration only when a private server-side key is configured.

Sprint 17 extends Basic mode with a separate opt-in public-knowledge retriever. It sends only a shortened, screened topic to the English Wikipedia API, returns an attributed extract and never presents a source-free lookup as fact.

The public status endpoint reports the active level. The interface uses this response to display **Knowledge assistant active** or **Advanced AI workspace**, so the deployment never claims capabilities it does not have.

## Supported basic guidance

- Computers, AI, machine learning, algorithms, databases, APIs, cloud computing and cybersecurity.
- Introductory Java, C#, Python, SQL, HTML, CSS, JavaScript, Spring Boot and REST APIs.
- Loops, methods, classes, objects, collections, object-oriented programming, debugging and unit testing.
- Agile development, Git, debuggers, MoSCoW prioritisation, risk registers and final-year-project aims and objectives.
- UniCircle connection, messaging and privacy workflows.
- Simple two-number arithmetic.
- Research planning and safer source-checking guidance.
- Image-prompt planning without falsely claiming that an image was rendered.

## Safety and honesty

The assistant warns users not to enter passwords or Authenticator codes. It does not complete assessed work on a student's behalf. Public retrieval is off until the user chooses it, obvious personal and secret data is blocked, and the interface explains which public service receives the shortened topic. It does not claim to verify changing university facts, understand arbitrary large programs or generate an image in no-cost mode. Unsupported questions receive a clear boundary rather than an invented answer.

## Acceptance evidence

- The status endpoint reports that the basic assistant is enabled when no external key exists.
- A guest can ask “What is a computer?” and receive a useful response.
- A guest can ask a beginner Spring Boot question and receive structured guidance.
- A guest can ask a simple arithmetic question and receive the correct result.
- Requests remain rate-limited and no university credentials are required.
- The full automated verification suite passes before deployment.

## Future extension

A university could later host an approved local model or configure the existing advanced gateway. That change would expand the answer range without redesigning the user interface or exposing provider credentials to the browser.
