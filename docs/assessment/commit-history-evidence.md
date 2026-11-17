# UniCircle commit-history evidence

**Audit date:** 17 November 2026

**Working branch:** `feat/unicircle-friends-pilot`

**Range reviewed:** 44 commits unique to `main..feat/unicircle-friends-pilot`
**Audit basis:** Commit dates, hashes and titles are reproduced from Git history. Main changed areas are summarised from the commit diffs.

This map explains the genuine development sequence. It does not imply that every commit passed every test at its original time. Verification evidence is stated conservatively and refers to the repository or dated documents that currently substantiate the work.

## Development stages

| Stage | Commits | Evidence represented |
| --- | --- | --- |
| 1. Problem definition and project setup | `1063e0a`, `7545c78` | Spring Boot foundation, campus UI structure and frontend structure tests |
| 2. Identity and university context | `dc5196a`-`94d1e71` | Profiles, security configuration, academic context and demo sign-in |
| 3. Core social and engagement implementation | `d9fdba8`-`5149a0e` | Connections, messaging, safety, events, bookings, opportunities and lost-property |
| 4. Staging-pilot and campus experience | `66d8fc9`-`e3c29fd` | Pilot accounts, mobile campus experience, updates, support, live learning and acceptance test centre |
| 5. Pilot fixes and responsive work | `b8620d5`-`572cb42` | Registration errors, invitation behaviour, account testing and UI routing fixes |
| 6. Responsible assistant and deployment hardening | `7120297`-`d9673b1` | Optional assistant, guest access, PWA deployment controls and security hardening |
| 7. Assistant and mobile expansion | `c2266dc`-`dd5e615` | No-cost assistant, public-source fallback, illustration studio and mobile workspaces |
| 8. Responsive QA and Test Campus administration | `74db5ca`-`68e52f7` | Browser evidence, release evidence, administration, password recovery and schedule integration |
| 9. Call diagnostics and hardening | `fbcf355`-`c29fc81` | Publishing fixes, call diagnostics, JavaScript call tests and relay documentation |
| 10. Research, architecture and assessment evidence | `ebb30a1`-`69adf34` | Safeguards, traceability, research, architecture and PDF evidence packs |
| 11. README completion | `4e9d876` | Assessor-facing README structure and conflict cleanup |

## Commit map

| Hash | Date | Commit title | Main areas changed | Stage and objective | Verification status |
| --- | --- | --- | --- | --- | --- |
| `1063e0a` | 8 Oct 2026 | Establish UniCircle Spring Boot foundation | Maven, Spring Boot, base UI, CI, compose configuration | Project setup and initial implementation | Foundation commit; subsequent full-suite evidence must be used for current status |
| `7545c78` | 8 Oct 2026 | Add premium campus workspaces and icon system | Dashboard HTML/CSS/JavaScript, sprint 01, frontend structure tests | Initial campus interface | Frontend structure tests exist |
| `dc5196a` | 8 Oct 2026 | Add secure identity and profile foundation | Security, profile identity, migrations and sprint 02 | Authentication and profile foundations | Security/profile tests added in later and current suite |
| `daa839d` | 8 Oct 2026 | Add university academic context boundary | Campus context packages, migration and sprint 03 | Server-derived university context | Campus-controller tests exist |
| `94d1e71` | 9 Oct 2026 | Add authenticated demo student journey | Demo sign-in, profile journey and sprint 04 | Fictional demo authentication | Demo journey tests exist |
| `d9fdba8` | 9 Oct 2026 | Add consent-based connections and messaging | Social state model, migration and sprint 05 | Connection consent and messaging foundation | Social and pilot journey tests exist |
| `8e292a9` | 9 Oct 2026 | Add realtime messaging and safety controls | WebSocket, notifications, safety, migration and sprint 06 | Realtime communication and blocking/reporting | Social, signalling and safety tests exist |
| `b1c8cbe` | 9 Oct 2026 | Add campus events and service bookings | Engagement package, migrations and sprint 07 | Event and appointment workflows | Engagement controller tests exist |
| `c7c495f` | 9 Oct 2026 | Add verified opportunities and application tracking | Opportunity package, migration and sprint 08 | Fictional opportunity catalogue and tracking | Opportunity controller tests exist |
| `a19f819` | 9 Oct 2026 | Build moderated lost-and-found sandbox | Lost-found package, media handling and sprint 09 | Moderated lost/property reporting | Lost-found controller and media tests exist |
| `5149a0e` | 9 Oct 2026 | Add auditable fictional staff review | Staff-review workflow, audit state and sprint 10 | Lost-found moderation and audit | Staff-review tests exist |
| `66d8fc9` | 12 Oct 2026 | Add invitation-only friends pilot with live calls | Pilot security, WebRTC, Docker and friends-pilot guide | Controlled staging pilot | Pilot, signalling and call configuration tests exist |
| `2109bca` | 12 Oct 2026 | Expand pilot into mobile campus experience | Mobile UI, pilot registration and sprint 11 | Mobile and account-pilot expansion | Frontend and pilot journey tests exist |
| `1b66c8d` | 12 Oct 2026 | Add campus updates support and live learning | Information module, migration and sprint 12 | Announcements, support and live learning | Information and structure tests exist |
| `e3c29fd` | 12 Oct 2026 | Add staging acceptance test centre | Evaluation module and sprint 13 | Persisted staging scenarios | Evaluation tests exist |
| `b8620d5` | 12 Oct 2026 | Preserve pilot registration errors | Pilot security and registration tests | Fix regression in pilot feedback | Covered by pilot journey tests |
| `4345d2a` | 12 Oct 2026 | Remove pilot invitation requirement | Pilot registration and data configuration | Follow-up pilot usability change | Pilot tests exist; the explicit requirement boundary remains documented |
| `7c347b9` | 12 Oct 2026 | Clarify two-account social testing | Frontend pilot/login presentation | Improve staged social test usability | Frontend structure checks exist |
| `d262455` | 12 Oct 2026 | Polish responsive campus experience | CSS, imagery and frontend tests | Responsive UI work | Frontend structure and recorded viewport checks |
| `547974a` | 12 Oct 2026 | Serve campus imagery across security profiles | Security resource access and journey tests | Fix UI asset delivery | Demo/pilot journey tests exist |
| `572cb42` | 12 Oct 2026 | Route signed-out profile actions to sign-in | Frontend signed-out routing | Security-aware navigation fix | Frontend structure tests exist |
| `7120297` | 12 Oct 2026 | Add responsible AI study assistant | Assistant package, provider boundary and sprint 14 | Optional campus assistant | Assistant controller tests exist |
| `4abb3b4` | 12 Oct 2026 | Allow guest access to Ask UniCircle | Assistant and security configuration | Guest assistant journey | Assistant/security tests exist |
| `d9673b1` | 13 Oct 2026 | Harden UniCircle deployment pilot | PWA, security headers, pilot deployment and sprint 15 | Deployment and browser hardening | Frontend and application tests exist |
| `c2266dc` | 13 Oct 2026 | Add no-cost basic UniCircle assistant | Local deterministic assistant and sprint 16 | No-cost assistant fallback | Assistant controller tests exist |
| `b1f42e6` | 13 Oct 2026 | Answer basic programming language questions | Assistant engine and tests | Expand deterministic programming guidance | Assistant tests cover the correction |
| `b2aa25a` | 13 Oct 2026 | Add sourced public knowledge assistant | Public-source assistant, consent and sprint 17 | Optional explicit public lookup | Assistant tests exist |
| `166e23c` | 13 Oct 2026 | Polish mobile shell and add private image studio | Mobile CSS, illustration and sprint 18 | Private SVG illustration and mobile navigation | Frontend and assistant tests exist |
| `72c4ad4` | 13 Oct 2026 | Complete focused mobile workspaces and assistant history | Assistant conversation storage and sprint 19 | Mobile feature workspaces and history | Assistant/profile/frontend tests exist |
| `dd5e615` | 13 Oct 2026 | Fix feature workspace loading states | Frontend loading and service-worker assets | UI navigation fix | Frontend structure tests exist |
| `74db5ca` | 14 Oct 2026 | Harden guest states and responsive experience | Guest UI, CSS and frontend tests | Improve signed-out and responsive state | Frontend structure tests |
| `6feac11` | 14 Oct 2026 | Complete responsive QA and release evidence | QA report and screenshots | Record manual responsive/browser evidence | QA report is evidence, not an independent audit |
| `3d6a425` | 14 Oct 2026 | Build the UniCircle Test Campus administration layer | Admin package, migrations and sprint 20 | Privileged test-campus workflows | Admin controller tests exist |
| `9b668e1` | 14 Oct 2026 | Add secure staging password recovery | Pilot recovery and migration | Account-recovery workflow | Pilot and admin journey tests exist |
| `3e0a42f` | 14 Oct 2026 | Build complete Test Campus admin control centre | Expanded admin operations and UI | Administration and audit workflows | Admin controller tests exist |
| `68e52f7` | 14 Oct 2026 | Connect campus schedules and live session audiences | Schedule, meeting audience migration and data fixtures | Multi-user schedule evidence | Engagement/information tests exist |
| `fbcf355` | 14 Oct 2026 | Fix campus publishing and notification delivery | Admin, information and registration fixes | Publishing/notification correction | Controller tests cover affected paths |
| `1b77296` | 14 Oct 2026 | Improve call connection diagnostics | Frontend call UI and service-worker | Diagnose WebRTC/relay status | Call JavaScript tests exist in later commit |
| `c29fc81` | 16 Oct 2026 | Harden call answering, audio playback and relay configuration | Call JavaScript, tests and calling guide | Fix call lifecycle and relay diagnostics | 16 Node call-logic tests pass in current verification |
| `ebb30a1` | 27 Oct 2026 | Strengthen assessment evidence and production safeguards | Architecture, traceability, manifest reconciliation and Docker | Evidence integrity and release safeguards | Maven verification required for current status |
| `d6ddbf6` | 9 Nov 2026 | Document research evidence and requirements baseline | Research, requirements and scope documents | Requirements and literature evidence | Document-only commit; no fabricated primary findings |
| `5e632f3` | 17 Nov 2026 | Align architecture boundaries and Windows run guide | Architecture docs, package names, Windows guide and PDF | Documentation and architecture alignment | Link/build commands should be rechecked |
| `69adf34` | 17 Nov 2026 | Archive research and requirements evidence pack | PDF evidence archive | Assessment evidence packaging | PDF existence verified |
| `4e9d876` | 17 Nov 2026 | Complete UniCircle README and resolve merge conflict | README only | Assessor/developer entry point | README link and conflict checks pass |

## Concerns and review notes

- The first commits include retained historical project or generated material, but the later branch deliberately establishes UniCircle as a separate active direction.
- Commit titles are generally descriptive. A few UI-only fixes do not state a test objective but the changed files and later sprints make the stage clear.
- Historical full-suite dates and counts must not be used as current verification evidence without re-running; this audit records the clean re-run instead.
- No real-credential pattern was identified in tracked files. `.env.example` contains placeholders only.
- Responsive screenshots and browser checks are evidence of named viewport checks, not independent WCAG conformance or physical-device testing.
- Participant findings, ethical approval and institutional production approval are not established by the commit history.
