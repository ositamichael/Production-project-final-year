# UniCircle literature review

**Review date:** 6 November 2026
**Project:** UniCircle, a secure digital campus platform for university life

## 1. Introduction

University students now depend on a mixture of institutional and commercial digital platforms to organise study, communicate with peers, find information and use campus services. This creates opportunities for flexible access and peer support, but it can also produce fragmented communication, duplicated effort, privacy risks and pressure to remain constantly available. UniCircle investigates whether selected university-life journeys can be presented through one secure and accessible interface without removing user choice or pretending to replace official university systems.

This review examines five questions that directly affect the project. First, what evidence exists that students experience friction when moving between digital platforms and campus services? Second, what is known about student connection, engagement and belonging? Third, what has previous research shown about university mobile applications and their evaluation? Fourth, which privacy, security and accessibility principles constrain an integrated campus platform? Finally, what gap remains for UniCircle to investigate?

The central conclusion is qualified. Existing evidence supports investigating a joined-up campus interface, especially for information discovery, peer connection, events, bookings and support. It does not prove that Leeds Beckett students experience the same problems, that they want the proposed feature set, or that UniCircle will improve belonging or retention. Those questions require local, ethics-approved evaluation. The literature therefore supplies a rationale and engineering constraints, not a guaranteed product outcome.

## 2. Review method

The review extends the initial literature search recorded in the Component 1 project specification. Searches conducted on 6 November 2026 used combinations of terms including `higher education student platform fragmentation`, `digital campus student experience`, `student belonging university retention`, `higher education mobile application usability`, `information systems success usability`, `privacy by design student platform`, `WCAG 2.2`, and `web application security verification standard`. Discovery searches were followed to the original journal, DOI, institutional repository, regulator or standards-body page. The search record and source decisions are retained in `source-evaluation-log.md`.

Sources were selected when they met at least one of four purposes: empirical or theoretical research about students and digital higher education; empirical research about university applications and evaluation; official UK higher-education evidence; or authoritative engineering, accessibility, privacy and security guidance. A source was not treated as academic evidence merely because it appeared in a search result. News articles, anonymous pages, vendor marketing and untraceable summaries were excluded from the argument.

Older sources were retained only where they define an established model, such as the Technology Acceptance Model, the Information Systems Success Model or the System Usability Scale. Their age and original context are considered when applying them. Current standards and regulator guidance were checked against their official pages because these sources change over time.

This is a focused narrative review rather than a systematic review. It did not search every university-library database, calculate inter-rater agreement or conduct a formal risk-of-bias assessment. A final dissertation search should use the Leeds Beckett Library to repeat the terms in IEEE Xplore, ACM Digital Library, Scopus or Web of Science where available, then record the number screened and reasons for exclusion. This limitation matters because the present review can support project decisions, but it cannot claim complete coverage of all research on digital campuses or student belonging.

## 3. Students in a platform based university

The current UK sector picture is not one of universal digital failure. Jisc's 2024/25 survey received 15,398 responses from 30 higher-education organisations, including 24 universities. Eighty-six per cent rated their digital learning environment above average and 84 per cent rated the digital learning experience on their course positively (Jisc, 2025). This challenges any simplistic claim that universities' digital services are generally poor. However, the same report records continuing requests for improved platform usability, clearer communication, more consistent support and stronger community. It also reports inequalities in device and connectivity access. These findings make usability and inclusion legitimate concerns, while showing that UniCircle needs a narrower problem statement than “university systems do not work”.

Jisc's survey has scale and UK relevance, but it represents 9% of UK higher-education providers and participation was uneven: nine organisations contributed fewer than 100 responses, while one contributed 5,550 (Jisc, 2025). It primarily concerns digital learning rather than the exact combination of social, event, booking and service journeys proposed for UniCircle. It is therefore suitable for establishing sector context, not for estimating demand at Leeds Beckett.

Noteboom's (2025) study gives more direct evidence about fragmentation. Twenty-two students at a Scottish research-intensive university attended one of five platform-mapping workshops and 14 completed follow-up interviews. Participants described moving between official university systems and services such as WhatsApp, WeChat, Discord, YouTube and Reddit. The study identified useful peer support and student agency, but also distraction, notification overload, blurred boundaries and divisions between groups using different platform ecosystems. This evidence is especially relevant because it focuses on lived platform use rather than institutional assumptions about what students use.

The study also has clear limits. It was exploratory, used a small voluntary sample from one research-intensive university and centred its published analysis on three detailed accounts. The findings cannot be generalised to every university or converted into a claim that one new platform will remove fragmentation. Indeed, creating another platform could make the problem worse if it duplicates existing channels, generates excessive notifications or pressures students to participate. For UniCircle, the study therefore supports restrained consolidation: a small number of well-defined journeys, user-controlled notification and visibility settings, and interfaces to official services rather than an attempt to reproduce every system.

The Jisc intelligent-campus guide similarly describes how connected data and applications can support navigation, facilities, events, social connection and student services (Clay, Hartland and Allen, 2023). However, it is professional guidance rather than an experimental product evaluation. Its most useful contribution is the warning that integration must have a clear user benefit and must address privacy, security, data quality, ethics and the risk of misinterpreting data. This creates an important design test: a feature should not be integrated simply because integration is technically possible.

## 4. Engagement connection and belonging

UniCircle includes circles, connection requests, messaging and event discovery, so the project needs a sound account of why social connection is relevant. Kahu and Nelson (2018) describe student engagement as emerging at an educational interface where student and institutional factors interact. Their framework identifies self-efficacy, emotion, belonging and wellbeing as mechanisms that can influence engagement. This is more useful than treating engagement as a count of logins or messages because it recognises that an institution and a student jointly shape the experience.

The framework is conceptual. It does not test UniCircle or demonstrate that a digital social feature will cause engagement. It nevertheless helps define appropriate evaluation questions: whether students understand available opportunities, feel able to take part, can control interaction and perceive the journeys as useful. It also shows why raw activity should not be treated automatically as success. A high message count could represent useful connection, but it could also represent pressure, harassment or notification overload.

UK evidence from the What Works? Student Retention and Success programme also treats supportive peer relations, meaningful interaction and a sense of belonging as important aspects of student success (Thomas et al., 2017). Its second phase involved 13 UK universities and sustained institutional work rather than a single application. The report repeatedly urges caution in interpreting belonging data because response rates, cohort sizes and institutional contexts varied. It supports the importance of proactive and inclusive opportunities, but it cannot isolate a software effect from curriculum, teaching, finance, personal circumstances and wider institutional practice.

More recent quantitative research strengthens the association while preserving this caution. Yang et al. (2026) synthesised 83 correlational studies involving 93,461 postsecondary students. Belonging had moderate correlations with academic motivation, interest and self-efficacy, but smaller correlations with performance, retention and engagement. Heterogeneity was high and most included research was cross-sectional. The authors therefore could not establish that belonging caused the outcomes. This distinction is essential for UniCircle: the project may investigate perceived connection and usefulness, but a small final-year evaluation cannot claim that the platform improves academic performance or retention.

Pedler, Willis and Nieuwoudt (2022) surveyed 578 Australian university students and found that students considering leaving reported a lower sense of belonging, while greater belonging was associated with motivation and enjoyment. The cross-sectional questionnaire again shows association rather than causation, and the Australian setting limits direct transfer to a UK university. Taken together, these studies justify designing respectful opportunities for connection, but not making belonging compulsory or describing social participation as a technical problem with a technical cure.

These findings lead to practical requirements. Connections should require acceptance; blocking and reporting must be available; private messages must be restricted to authorised participants; and social discovery should not expose sensitive profile or location information. Evaluation should ask whether these controls are understood and whether users feel able to decline participation. Success is not simply more interaction, but useful interaction with meaningful user control.

## 5. Evidence from higher education applications

Research on existing higher-education applications shows that evaluation must examine tasks rather than screenshots. Nik Ahmad and Hussaini (2021) evaluated the UniKL Link mobile application with 64 undergraduate and postgraduate students at Universiti Kuala Lumpur. Using effectiveness, efficiency and satisfaction measures, they reported an overall satisfaction rate of 82.15%, while still identifying usability problems and recommendations. The study demonstrates a workable evaluation structure: prepare realistic tasks, observe performance on participants' own devices and combine quantitative measures with reported problems.

Its findings cannot be used as a target result for UniCircle. Participants were subjectively selected from one Malaysian institution, 57 of the 64 were undergraduates, and the tested product and institutional context differ from this project. The percentage also summarises satisfaction rather than proving accessibility, security or long-term adoption. Its value lies in method, particularly the separation of effectiveness, efficiency and satisfaction.

Dobbins and Denton (2017) studied MyWallMate, an application supporting student interaction during lectures at Liverpool John Moores University. The study involved 347 students across four science modules and was approved by the institution's research ethics committee. Students used the application to submit questions and responses through a lecture interaction system. Reported responses were positive, but participation in individual sessions and questionnaire items varied, and some reported percentages were based on much smaller response counts than the total cohort. The setting was also limited to lecture interaction rather than the broader campus platform proposed by UniCircle.

The study remains valuable for two reasons. First, it shows a UK university application evaluated in a real educational setting rather than only in a laboratory. Second, it demonstrates why the final report must state the denominator for every percentage and distinguish the number exposed to a system from the number who supplied each result. UniCircle should retain task-level raw data, sample size, completion, errors, assistance and questionnaire responses instead of reporting only a favourable headline percentage.

The Technology Acceptance Model proposes that perceived usefulness and perceived ease of use influence acceptance (Davis, 1989). These constructs remain relevant to a voluntary campus platform, but the model was developed in an older workplace-computing context and intention is not the same as successful task completion. DeLone and McLean's (2003) Information Systems Success Model adds system quality, information quality, service quality, use, user satisfaction and net benefits. This broader view is better suited to UniCircle because a visually usable platform can still fail if its information is unreliable, access controls are weak or its services do not meet a real need.

The System Usability Scale provides a short standardised measure of perceived usability (Brooke, 1996). Bangor, Kortum and Miller (2008) analysed SUS data from 206 industrial usability studies and showed how scores can be interpreted empirically. SUS is useful after participants complete defined tasks, but it is not diagnostic and does not establish accessibility, security or usefulness. The Component 1 target of a mean SUS score of 70 is therefore a project benchmark, not a university requirement and not proof that the product is successful. It should be reported with sample size, score distribution and task evidence.

## 6. Comparative real time communication models

UniCircle uses real-time communication for messaging, presence and call signalling, so the transport choice needs more than the claim that WebSockets are “faster”. Conventional HTTP is a stateless request-response protocol, but HTTP/1.1 normally reuses persistent connections and HTTP/2 can multiplex concurrent streams on one connection (Fielding, Nottingham and Reschke, 2022; Thomson and Benfield, 2022). It is therefore inaccurate to assume that every REST request creates a new TCP connection or that polling automatically causes server thread-pool starvation. The actual cost depends on polling frequency, payloads, connection reuse, server architecture and client population.

Short polling is simple and works with ordinary HTTP infrastructure, but it repeatedly asks for updates even when nothing has changed. Long polling reduces empty requests by holding a request until data is available, although the client must issue another request after each response or timeout. Server-Sent Events use the standard `EventSource` interface and a persistent `text/event-stream` response to deliver server-to-client events (WHATWG, 2026). SSE includes useful browser behaviour such as automatic reconnection, but the EventSource channel is one-way. A client can still send changes through normal HTTP requests, so SSE is not incapable of interactive systems; it simply divides client-to-server commands and server-to-client updates across different mechanisms.

RFC 6455 defines WebSocket as two-way communication established by an opening HTTP Upgrade handshake and then continued through framed messages on the connection (Fette and Melnikov, 2011). This suits UniCircle because messaging and call signalling require frequent events in both directions and because the existing Spring gateway already authenticates sessions and routes messages to accounts. The decision is based on interaction shape and implementation coherence, not an unmeasured promise of “raw processing speed”. WebSocket also adds obligations: connection authentication, origin restrictions, message-level authorisation, payload limits, rate limits, heartbeat or timeout handling, reconnection backoff and behaviour across proxies or multiple server instances.

The appropriate comparison is therefore conditional. REST remains suitable for durable resource creation, history retrieval and ordinary page data. SSE would be a reasonable alternative for server-only notifications. WebSocket is justified for the bidirectional transient events in messaging and signalling, while durable messages should still be persisted through controlled application services. The final engineering evidence must measure connection success, message delivery time, failures and resource use under a stated workload. Until those measurements exist, the project can justify the protocol choice but cannot claim a performance advantage.

## 7. Location privacy spatial abstraction and retention

Location information can reveal routines, relationships and sensitive places even when obvious names are removed. Gruteser and Grunwald (2003) proposed spatial and temporal cloaking that adapts the disclosed area or time interval until a specified anonymity set is achieved. Their model demonstrates an important principle: privacy depends on the other people, density, time and attacker knowledge, not only the number of decimal places in a coordinate. Wernke et al. (2014) similarly classify a range of location attacks and protection approaches, showing that mechanisms protect different information under different threat assumptions.

Geo-indistinguishability applies a formal differential-privacy idea to location by adding calibrated planar noise so nearby locations become difficult to distinguish within a stated privacy radius (Andrés et al., 2013). This supplies a stronger mathematical privacy definition than fixed rounding, but it creates a privacy-utility trade-off and requires a defensible privacy parameter and query model. It is not automatically the best solution for a short-lived mutual meet-up feature, especially where the application does not publish aggregate location queries.

UniCircle currently rounds latitude and longitude to four decimal places. One decimal degree of latitude is approximately 111 kilometres, so 0.0001 degrees is roughly 11 metres north-south; longitude distance varies with latitude. Rounding therefore reduces precision but does not create an “unbreakable bounding box”, k-anonymity or differential privacy. A rounded point may still identify a building, residence or individual, and repeated observations can reveal movement. The control must be described honestly as precision reduction within a wider minimisation design.

For the prototype, the more defensible control set is voluntary sharing, mutual accepted-connection checks, short automatic expiry, immediate revocation, physical deletion of expired rows, restricted visibility and no permanent history. The ICO's data-protection-by-design guidance supports limiting collection, processing, retention and accessibility to the defined purpose (ICO, 2026). The next implementation decision should define the least precision actually needed for a campus-zone meet-up. If users only need a named campus zone, storing a zone identifier instead of coordinates would reduce risk further. Four-decimal coordinates should be retained only if a tested user journey demonstrates that zone-level information is insufficient.

Testing must include invalid coordinates, sharing being off by default, blocked and unconnected users, one-sided sharing, mutual sharing, immediate stop, expiry at the time boundary, physical purge and audit of any privileged access. Privacy evaluation should also consider inference: whether a sequence of permitted responses reveals more than one response alone. None of these controls amounts to legal approval; a live service would still require institutional governance, a lawful-basis decision and an assessment of whether a data protection impact assessment is necessary.

## 8. Access control object ownership and secure media handling

Role-Based Access Control assigns permissions through organisational roles and can simplify administration where roles are stable (Sandhu et al., 1996). UniCircle's student, staff and administrator roles fit this model for broad functions, but roles alone are insufficient. Two students have the same role while owning different profiles, messages, support cases and bookings. The system therefore needs role checks plus object ownership, university context, relationship state and resource-specific rules.

OWASP identifies Broken Object Level Authorization as the failure to verify that the current user may act on the specific object identified in an API request (OWASP Foundation, 2023). Moving checks into middleware does not automatically prevent this weakness because generic middleware may know the role but not the requested conversation, booking or report. UniCircle must deny by default and validate permissions at the protected operation or service where the requested object and current account are both available. Tests should change identifiers, roles and university context and verify denial without disclosing whether an inaccessible object exists.

Uploaded images create a separate attack surface. The risk is broader than EXIF metadata or “buffer strings”. An attacker may spoof a content type, upload an oversized or malformed file, exploit a vulnerable decoder, place active content in a publicly served file, overwrite another file or exhaust storage. OWASP recommends allow-listed formats, validation that does not trust the client Content-Type header, generated filenames, size limits, authorisation, storage outside the web root, current processing libraries and malware or sandbox scanning where proportionate (OWASP Foundation, 2026a).

The existing UniCircle storage code decodes an image and re-encodes it into an allowed format. This is a meaningful control because it rejects undecodable input and normally removes unnecessary source metadata, including EXIF. It is not a complete “binary abstraction layer” and cannot guarantee that every malicious file is harmless. The decoder itself processes untrusted input and must have restricted formats, dimension and resource limits and supported dependencies. The application should verify generated names, non-executable storage, safe response Content-Type, `nosniff`, access rules, maximum byte size, maximum pixel dimensions, corrupted images, extension and MIME mismatches, duplicate names and storage exhaustion behaviour. Production deployment should also decide whether antivirus scanning is necessary for the accepted formats and risk level.

## 9. Privacy security and accessibility constraints

An integrated platform can increase the consequence of unauthorised access because profiles, relationships, messages, bookings, support requests and optional location data are connected. The Information Commissioner's Office states that data protection by design and by default must begin at the planning stage and continue through the processing lifecycle (ICO, 2026). It requires organisations to limit the amount of personal information, extent of processing, storage period and accessibility to what is necessary for each purpose. Applied to UniCircle, this supports fictional development data, clear purposes for each field, short retention for temporary location, immediate revocation, restricted administrative access and documented deletion rules.

These controls demonstrate privacy-aware engineering; they do not prove legal compliance or institutional approval. A real deployment would require the university to confirm controller responsibilities, lawful basis, retention, security review and whether a data protection impact assessment is required. The prototype must therefore avoid live student records and must not present its test identity or location functions as approved university services.

Security guidance also needs to be used at the correct level. The OWASP Top 10 is an awareness document, whereas the OWASP Application Security Verification Standard 5.0.0 provides testable requirements for application controls (OWASP Foundation, 2025). UniCircle should use ASVS identifiers to define a proportionate verification set covering authentication, access control, input handling, sessions, logging and data protection. Because the platform has student, staff and administrator roles, broken object-level or role-level access is a more serious risk than a purely visual defect. Negative tests must show that users cannot read another account's private information, join unauthorised conversations or perform privileged actions.

OpenID Connect defines an identity layer over OAuth 2.0 and supplies an appropriate technical boundary for a future institutional identity provider (OpenID Foundation, 2023). It does not grant access to Leeds Beckett single sign-on. The current prototype should therefore describe its fictional accounts honestly and keep external identity behind an adapter so that an authorised provider could be integrated later without redesigning every module.

Accessibility is also part of the engineering baseline rather than an optional polish stage. WCAG 2.2 is a W3C Recommendation covering perceivable, operable, understandable and robust content (W3C, 2024). Relevant requirements include keyboard access, visible and unobscured focus, adequate target size, error identification, accessible authentication and meaningful status messages. Automated scanners can identify some failures, but cannot establish full conformance. UniCircle should state that it targets WCAG 2.2 AA on named core journeys until automated and manual keyboard and assistive-technology checks have been completed and defects retested.

## 10. Synthesis and research gap

The literature reveals a tension rather than a simple product opportunity. Students benefit from digital flexibility, informal peer support and access to varied services. At the same time, moving across platforms can create distraction, notification overload, blurred personal boundaries, unequal access and uncertainty about data. A unified interface may reduce steps for selected journeys, but it may also become another compulsory channel or concentrate too much personal information. UniCircle must be evaluated against both possibilities.

The strongest justification for the project is therefore not that all university services should be merged. It is that a carefully bounded prototype can test whether selected, commonly related journeys are easier to discover and complete when they share a consistent interface and permission model. The literature supports the relevance of the investigation and supplies criteria for privacy, security, accessibility and evaluation. It does not identify which UniCircle features Leeds Beckett students value most, how they currently complete those tasks, or whether the implemented prototype reduces effort.

The resulting research gap is local and practical: there is insufficient evidence about whether the proposed combination of campus information, consensual peer connection, events, bookings and support improves defined university-life tasks for the intended users without creating unacceptable privacy, safety or accessibility problems. Subject to favourable ethical approval, this gap can be investigated through task-based usability testing and qualitative feedback. Until that evidence exists, the project should report the problem as a research question rather than a proven need.

## 11. Implications for UniCircle

The literature supports the following decisions:

1. Keep the assessed product focused on defined journeys rather than attempting to replace the VLE, student record system, emergency services or every communication channel.
2. Preserve voluntary participation, connection approval, blocking, reporting, notification control and clear social boundaries.
3. Use fictional accounts and test adapters unless institutional access is formally authorised.
4. Minimise personal data, make temporary location sharing opt-in, restrict visibility and enforce automatic expiry and deletion.
5. Evaluate effectiveness, efficiency, errors, assistance, perceived usability, usefulness and qualitative concerns rather than relying on one satisfaction score.
6. Treat accessibility and negative security tests as acceptance evidence for core journeys.
7. Avoid claims about improved retention, belonging, legal compliance, institutional approval or production readiness unless evidence directly supports them.
8. Use REST for durable resource operations and WebSocket only where bidirectional live events are justified; demonstrate performance through measurement rather than protocol assumptions.
9. Describe four-decimal coordinates as precision reduction, not anonymisation, and reassess whether a campus-zone identifier can replace coordinates.
10. Combine role checks with object ownership, relationship and university-context checks, then test changed identifiers and cross-role access.
11. Treat image re-encoding as one upload control within a wider validation, storage, resource-limit and dependency-hardening strategy.

## References

Andrés, M.E., Bordenabe, N.E., Chatzikokolakis, K. and Palamidessi, C. (2013) 'Geo-indistinguishability: Differential privacy for location-based systems', in *Proceedings of the 2013 ACM SIGSAC Conference on Computer and Communications Security*, pp. 901-914. Available at: https://doi.org/10.1145/2508859.2516735 (Accessed: 6 November 2026).

Bangor, A., Kortum, P.T. and Miller, J.T. (2008) 'An empirical evaluation of the System Usability Scale', *International Journal of Human-Computer Interaction*, 24(6), pp. 574-594. Available at: https://doi.org/10.1080/10447310802205776 (Accessed: 6 November 2026).

Brooke, J. (1996) 'SUS: A quick and dirty usability scale', in Jordan, P.W., Thomas, B., McClelland, I.L. and Weerdmeester, B. (eds.) *Usability Evaluation in Industry*. London: Taylor and Francis, pp. 189-194.

Clay, J., Hartland, D. and Allen, R. (2023) *Building the future intelligent campus*. Jisc. Available at: https://www.jisc.ac.uk/guides/building-the-future-intelligent-campus (Accessed: 6 November 2026).

Davis, F.D. (1989) 'Perceived usefulness, perceived ease of use, and user acceptance of information technology', *MIS Quarterly*, 13(3), pp. 319-340. Available at: https://doi.org/10.2307/249008 (Accessed: 6 November 2026).

DeLone, W.H. and McLean, E.R. (2003) 'The DeLone and McLean model of information systems success: A ten-year update', *Journal of Management Information Systems*, 19(4), pp. 9-30. Available at: https://doi.org/10.1080/07421222.2003.11045748 (Accessed: 6 November 2026).

Dobbins, C. and Denton, P. (2017) 'MyWallMate: An investigation into the use of mobile technology in enhancing student engagement', *TechTrends*, 61, pp. 541-549. Available at: https://doi.org/10.1007/s11528-017-0188-y (Accessed: 6 November 2026).

Information Commissioner's Office (2026) *Data protection by design and by default*. Available at: https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/accountability-and-governance/guide-to-accountability-and-governance/data-protection-by-design-and-by-default/ (Accessed: 6 November 2026).

Fette, I. and Melnikov, A. (2011) *The WebSocket Protocol*. RFC 6455. Internet Engineering Task Force. Available at: https://www.rfc-editor.org/rfc/rfc6455 (Accessed: 6 November 2026).

Fielding, R., Nottingham, M. and Reschke, J. (2022) *HTTP Semantics*. RFC 9110. Internet Engineering Task Force. Available at: https://www.rfc-editor.org/rfc/rfc9110 (Accessed: 6 November 2026).

Gruteser, M. and Grunwald, D. (2003) 'Anonymous usage of location-based services through spatial and temporal cloaking', in *Proceedings of the First International Conference on Mobile Systems Applications and Services*, pp. 31-42. Available at: https://doi.org/10.1145/1066116.1189037 (Accessed: 6 November 2026).

Jisc (2025) *2024/25 UK higher education students digital experience insights survey findings*. Available at: https://digitalinsights.jisc.ac.uk/reports-and-briefings/our-reports/2024-25-uk-higher-education-students-digital-experience-insights-survey-findings/ (Accessed: 6 November 2026).

Kahu, E.R. and Nelson, K.J. (2018) 'Student engagement in the educational interface: Understanding the mechanisms of student success', *Higher Education Research and Development*, 37(1), pp. 58-71. Available at: https://doi.org/10.1080/07294360.2017.1344197 (Accessed: 6 November 2026).

Nik Ahmad, N.A. and Hussaini, M. (2021) 'A usability testing of a higher education mobile application among postgraduate and undergraduate students', *International Journal of Interactive Mobile Technologies*, 15(9), pp. 88-102. Available at: https://doi.org/10.3991/ijim.v15i09.19943 (Accessed: 6 November 2026).

Noteboom, J. (2025) 'The student as user: Mapping student experiences of platformisation in higher education', *Learning, Media and Technology*, 50(1), pp. 29-43. Available at: https://doi.org/10.1080/17439884.2024.2414055 (Accessed: 6 November 2026).

OpenID Foundation (2023) *OpenID Connect Core 1.0 incorporating errata set 2*. Available at: https://openid.net/specs/openid-connect-core-1_0-errata2.html (Accessed: 6 November 2026).

OWASP Foundation (2025) *OWASP Application Security Verification Standard 5.0.0*. Available at: https://asvs.owasp.org/ (Accessed: 6 November 2026).

OWASP Foundation (2023) *API1:2023 Broken Object Level Authorization*. Available at: https://api-security.owasp.org/editions/2023/en/0xa1-broken-object-level-authorization/ (Accessed: 6 November 2026).

OWASP Foundation (2026a) *File Upload Cheat Sheet*. Available at: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html (Accessed: 6 November 2026).

Sandhu, R.S., Coyne, E.J., Feinstein, H.L. and Youman, C.E. (1996) 'Role-based access control models', *Computer*, 29(2), pp. 38-47. Available at: https://doi.org/10.1109/2.485845 (Accessed: 6 November 2026).

Thomson, M. and Benfield, C. (2022) *HTTP/2*. RFC 9113. Internet Engineering Task Force. Available at: https://www.rfc-editor.org/rfc/rfc9113 (Accessed: 6 November 2026).

Pedler, M.L., Willis, R. and Nieuwoudt, J.E. (2022) 'A sense of belonging at university: Student retention, motivation and enjoyment', *Journal of Further and Higher Education*, 46(3), pp. 397-408. Available at: https://doi.org/10.1080/0309877X.2021.1955844 (Accessed: 6 November 2026).

Thomas, L., Hill, M., O'Mahony, J. and Yorke, M. (2017) *Supporting student success: Strategies for institutional change*. London: Paul Hamlyn Foundation. Available at: https://www.phf.org.uk/assets/documents/Full-report-Final.pdf (Accessed: 6 November 2026).

World Wide Web Consortium (2024) *Web Content Accessibility Guidelines WCAG 2.2*. W3C Recommendation, 12 December. Available at: https://www.w3.org/TR/WCAG22/ (Accessed: 6 November 2026).

Wernke, M., Skvortsov, P., Dürr, F. and Rothermel, K. (2014) 'A classification of location privacy attacks and approaches', *Personal and Ubiquitous Computing*, 18, pp. 163-175. Available at: https://doi.org/10.1007/s00779-012-0633-z (Accessed: 6 November 2026).

WHATWG (2026) *HTML Standard: Server-sent events*. Available at: https://html.spec.whatwg.org/multipage/server-sent-events.html (Accessed: 6 November 2026).

Yang, X., Zhang, D., Wang, X. and Samsudin, S.B. (2026) 'Sense of belonging and academic outcomes among postsecondary students: A meta-analysis', *Frontiers in Psychology*, 17, 1841185. Available at: https://doi.org/10.3389/fpsyg.2026.1841185 (Accessed: 6 November 2026).
