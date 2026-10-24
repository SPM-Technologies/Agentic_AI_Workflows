# Denied-Claim Appeal Assistant - demo

A small Spring MVC (no Boot) + LangChain4j + Claude demo of one AI workflow with **human approval**.
All data is **synthetic** - invented patients, fictional insurers. Nothing is really sent.

## In simple words
After a visit, a medical practice bills the patient's insurer (a **claim**). Sometimes the insurer refuses to pay
(a **denial**). The practice can ask the insurer to reconsider (an **appeal**) by sending a letter plus proof from the chart.
This demo prepares that letter and proof with AI; a biller reviews, edits and approves it.

## What it shows
| Step | Pattern | Where |
|---|---|---|
| Classify the denial reason | Routing | `DenialClassifier` |
| Coding denials go a different way (corrected claim, not appeal) | Conditional routing | `CorrectedClaimAdvisor` |
| Collect proof with read-only tools bound to ONE claim | Tools + guardrail | `ClaimTools` |
| Draft the appeal letter (or advise NOT to appeal) | Sequence | `AppealWriter` |
| Check EVERY draft (letter, "not recommended" note, corrected claim) against the **source records read by code** - never against the AI's own summary; fix and re-check, plus a final check if rounds run out | Loop (max 3) | `AppealChecker`, `AppealReviser` |
| Runs in the background; page refreshes itself | Async work queue | `AppealService` |
| Biller approves / edits / rejects | Human-in-the-loop | `appeal.jsp` |
| Every AI step, tool read and human decision logged | Audit trail | `Appeal.audit` |

## The four demo claims
| Claim | Denial | Expected AI result |
|---|---|---|
| C-1001 MRI | CO-50 not medically necessary | Appeal letter (strong evidence) |
| C-1002 Physical therapy visit 13 | CO-197 no authorization | **Not recommended** - request new authorization instead |
| C-1003 Office visit + injection | CO-4 missing modifier | **Corrected claim** - add modifier 25 |
| C-1004 Echocardiogram | CO-50 not medically necessary | Appeal letter (strong evidence) |

## Run it - only Docker needed (no Java or Maven on the machine)
PowerShell in this folder, Docker Desktop running:
```
# set the API key for this terminal only (hidden, not saved):
$env:ANTHROPIC_API_KEY = [Net.NetworkCredential]::new('', (Read-Host 'API key' -AsSecureString)).Password
docker compose up -d --build
```
Open http://localhost:8090/ (port 8090, so it can run next to the framework prototype on 8080).

After code changes: `docker compose up -d --build` again. Logs: `docker compose logs -f`. Stop: `docker compose down`.
Full documentation: [docs/Appeal-Assistant-Guide.html](docs/Appeal-Assistant-Guide.html). Free testing with a fake Claude server: guide section "Testing".

Cost: each prepared claim makes about 4-8 Claude calls (a few cents with Claude Opus 5.5).

## How to test (real Claude)
| # | Do | Expect |
|---|---|---|
| 1 | Open http://localhost:8090/ | Work list with 4 claims, status "New" |
| 2 | C-1001 -> **Prepare with AI** | "Preparing..." page refreshes; audit log shows live tool reads; after 1-2 min "Ready for review", routing MEDICAL_NECESSITY, letter citing dates/sources (X-ray, 6 PT sessions, neurological findings) |
| 3 | C-1002 -> Prepare | "Not recommended - review": visit 13 was beyond the 12 authorized visits; suggests requesting a new authorization |
| 4 | C-1003 -> Prepare | "Corrected claim suggested - review": add modifier 25 to 99213 (separate headache problem documented) |
| 5 | C-1004 -> Prepare, edit one line, **Approve** | Status "Approved - sent (simulated)"; audit says "Approved with edits" |
| 6 | C-1002 -> **Reject** with a reason | Status "Rejected"; "Prepare again" appears; audit keeps the full history |
| 7 | Open "AI steps" on any claim | Each agent's output, incl. checker results ("NONE" = all statements supported) |

Small differences in wording between runs are normal - the path (appeal / not recommended / corrected claim) should match.

## Demo limitations (deliberate)
In-memory data (lost on restart) - no login, single demo user `biller.demo` - no CSRF protection - sending is simulated.
A real product would use the existing database, Spring Security login/CSRF, and the real claim submission service.

## Code map
```
config/ClaudeModel.java        shared Claude connection (adaptive thinking, drop_block, fallbacks)
data/DemoData.java             SYNTHETIC claims, patients, chart notes, authorizations, insurer policies
tools/ClaimTools.java          read-only tools, bound to one claim; records every call for the audit log
agents/AppealAgents.java       the 6 agents (one job each)
service/AppealWorkflow.java    routing -> (corrected claim | proof -> letter -> check/revise loop)
service/AppealService.java     work queue: background preparation, approve / reject, audit
service/Appeal.java            one work-queue item + statuses + audit events
controller/AppealController.java  /, /claims/{id}/prepare, /appeals/{id}, approve, reject, /health
views/worklist.jsp, appeal.jsp
```
