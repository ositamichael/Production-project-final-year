# UniCircle research source evaluation log

**Search date:** 6 November 2026
**Purpose:** retain a reproducible record of the sources used in the UniCircle literature review

## Search record

The following searches were completed using web discovery, followed by the original DOI, journal, institutional repository or official organisation page:

- `higher education student platform fragmentation`
- `digital campus student experience platform usability community`
- `student as user platformisation higher education fragmentation notifications`
- `student engagement belonging higher education retention`
- `sense of belonging postsecondary meta-analysis`
- `higher education mobile application usability testing`
- `university mobile application student engagement UK`
- `information systems success user satisfaction net benefits`
- `System Usability Scale empirical evaluation`
- `WCAG 2.2 Recommendation`
- `ICO data protection by design and default`
- `OWASP ASVS 5.0.0`
- `OpenID Connect Core errata set 2`
- `RFC 6455 WebSocket protocol full duplex persistent connection`
- `WHATWG server-sent events EventSource specification`
- `HTTP persistent connection HTTP/2 multiplexing RFC`
- `location privacy spatial temporal cloaking k-anonymity`
- `geo-indistinguishability differential privacy location`
- `role based access control Sandhu NIST`
- `OWASP broken object level authorization`
- `OWASP secure file upload image metadata re-encoding`

## Inclusion rules

A source was retained when its author and publication could be verified and it contributed empirical evidence, a recognised theoretical model, official UK sector evidence, a regulator requirement, a formal standard or first-party technical specification. Sources were excluded if they were anonymous, promotional, untraceable, unrelated to the actual UniCircle users or journeys, or repeated a claim without linking to the original work.

## Evaluated sources

| Source | Type and context | Method or authority | Main contribution | Limitation and permitted use |
| --- | --- | --- | --- | --- |
| Jisc (2025) | Official UK HE sector report | Survey of 15,398 respondents at 30 organisations; 24 universities and six HE colleges | Shows strong overall digital satisfaction alongside continued problems with usability, communication, support, access and community | Organisations represented 9% of UK providers and response counts were uneven. Use for UK context, not a Leeds Beckett demand claim. |
| Noteboom (2025) | Peer-reviewed UK qualitative study | Five workshops with 22 students at one Scottish university; 14 follow-up interviews; platform maps and abductive coding | Direct evidence of movement between official and unofficial platforms, peer support, distraction, notification overload, fragmentation and platform divisions | Small voluntary single-institution sample at a research-intensive university. Use to frame risks and research questions, not population prevalence. |
| Clay, Hartland and Allen (2023) | Jisc professional guide | Sector examples and implementation guidance | Explains potential of connected campus services and stresses user benefit, privacy, ethics, security and data quality | Not an experimental evaluation. Use for practical context and design cautions. |
| Kahu and Nelson (2018) | Peer-reviewed conceptual paper | Synthesis and refinement of student-engagement theory | Defines educational interface and identifies self-efficacy, emotion, belonging and wellbeing as mediating constructs | Does not test an application or establish causality. Use to frame evaluation constructs. |
| Thomas et al. (2017) | UK multi-institution programme report | Second phase of What Works? across 13 UK universities | Supports inclusive, proactive institutional work and the relevance of belonging and supportive relationships | Institutional interventions and variable survey responses prevent a software-specific causal claim. |
| Yang et al. (2026) | Peer-reviewed systematic review and meta-analysis | 83 correlational studies; 93,461 students; random-effects models; PRISMA process | Finds small-to-moderate associations between belonging and academic outcomes, strongest for motivational and psychosocial outcomes | High heterogeneity and predominantly cross-sectional studies. Use to support relevance, not causal product claims. |
| Pedler, Willis and Nieuwoudt (2022) | Peer-reviewed empirical study | Cross-sectional questionnaire with 578 Australian university students | Links lower belonging with considering leaving and higher belonging with motivation and enjoyment | Self-report, cross-sectional and Australian context. Use as supporting association only. |
| Nik Ahmad and Hussaini (2021) | Peer-reviewed application evaluation | Usability tasks with 64 subjectively selected Malaysian HE students using their own devices | Provides an effectiveness, efficiency and satisfaction evaluation structure and reports remaining defects despite positive satisfaction | One institution and non-random sample; product differs from UniCircle. Use for method, not expected results. |
| Dobbins and Denton (2017) | Peer-reviewed UK application study | 347 students exposed across four science modules; lecture interaction and questionnaire; institutional ethics approval | Demonstrates real-context UK evaluation of a student mobile application and the value of mixed evidence | Individual response counts were smaller and variable; lecture-specific setting. Report denominators and avoid generalising. |
| Davis (1989) | Foundational peer-reviewed model | Empirical development of perceived usefulness and perceived ease-of-use measures | Supplies acceptance constructs relevant to voluntary use | Older workplace context; perceptions and intention do not prove task success or sustained adoption. |
| DeLone and McLean (2003) | Foundational peer-reviewed model | Ten-year update of Information Systems Success Model | Connects system, information and service quality with use, satisfaction and net benefits | General model requiring project-specific operational definitions. |
| Brooke (1996); Bangor, Kortum and Miller (2008) | Usability instrument and empirical interpretation | Ten-item SUS; later analysis of 2,324 surveys from 206 studies | Supplies a compact perceived-usability measure and empirical interpretation | Subjective and non-diagnostic. Must be combined with tasks, errors, accessibility and security evidence. |
| ICO (2026) | UK regulator guidance | Current official guidance on UK GDPR Article 25 | Requires data protection through the lifecycle and default limitation of collection, processing, storage and access | Application depends on actual purposes, controller and risks. Does not by itself prove legal compliance. |
| OWASP ASVS 5.0.0 | Open web-application security standard | Stable requirements published 30 May 2025 | Supplies identifiers and testable controls for security requirements and verification | A proportionate subset must be selected and actually tested; mentioning ASVS is not evidence of security. |
| W3C WCAG 2.2 | W3C Recommendation | Normative accessibility success criteria | Supplies testable accessibility requirements for core web journeys | Automated testing covers only part of the standard. Do not claim conformance before manual and automated verification. |
| OpenID Connect Core 1.0 errata set 2 | OpenID Foundation specification | Approved identity specification over OAuth 2.0 | Supports a future interoperable authentication boundary | Does not provide or authorise Leeds Beckett SSO. Use fictional identity until an institutional provider is approved. |
| RFC 6455; WHATWG EventSource; RFC 9110 and RFC 9113 | Internet and web standards | Normative definitions of WebSocket, SSE, HTTP semantics and HTTP/2 | Support an accurate comparison of request-response, server push and bidirectional framed communication | Standards define protocols, not UniCircle performance. Any latency or capacity claim requires a controlled benchmark. |
| Gruteser and Grunwald (2003) | Peer-reviewed location-privacy research | Spatial and temporal cloaking adjusted to meet an anonymity-set constraint | Demonstrates that location anonymity depends on population, space, time and attacker knowledge | Traffic-based model and trusted broker differ from UniCircle. Fixed decimal rounding cannot be called k-anonymity. |
| Andrés et al. (2013) | Peer-reviewed privacy research | Formal geo-indistinguishability model based on differential privacy | Provides a mathematical alternative using calibrated location noise and an explicit privacy-utility trade-off | Requires a justified privacy parameter and service model; not automatically proportionate for a short-lived meet-up feature. |
| Wernke et al. (2014) | Peer-reviewed classification | Systematic classification of location attacks and protection mechanisms | Shows that different controls address different threat assumptions | Classification predates some current mobile systems. Use to structure threats, not as proof that one chosen control is sufficient. |
| Sandhu et al. (1996) | Foundational peer-reviewed security model | Formal family of RBAC models involving users, roles, permissions, hierarchies and constraints | Justifies roles for broad student, staff and administrator permissions | Roles do not express ownership, relationships or university context on their own. |
| OWASP API Security Top 10 2023 and Authorization Cheat Sheet | Application-security guidance | Community-reviewed awareness and implementation guidance | Defines BOLA and recommends deny-by-default, per-request and per-object checks | Awareness guidance is not verification evidence; tests must demonstrate enforcement for UniCircle objects. |
| OWASP File Upload Cheat Sheet | Application-security guidance | Threat model and layered upload controls | Supports allow-listing, decoding/re-encoding, size limits, generated names, safe storage and scanning decisions | Re-encoding and metadata removal reduce risk but do not guarantee a file or decoder is safe. |

## Evidence gaps requiring later work

1. Repeat the academic search through the Leeds Beckett Library and record databases, dates, results screened and exclusions.
2. Confirm whether newer peer-reviewed UK research changes the conclusions before the final report is submitted.
3. Do not collect student interviews, surveys or usability results until the approved ethics position permits it.
4. After approval, establish how intended users currently complete the selected tasks and which tasks have the greatest friction.
5. Retain anonymised raw results, denominators, unsuccessful tasks and negative feedback so the final evaluation can be audited.
